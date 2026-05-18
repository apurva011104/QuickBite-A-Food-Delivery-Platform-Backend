package com.quickbite.auth.authservice.service.serviceImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.quickbite.auth.authservice.dto.requestDto.LoginRequestDto;
import com.quickbite.auth.authservice.dto.requestDto.LoginType;
import com.quickbite.auth.authservice.dto.requestDto.RegisterRequestDto;
import com.quickbite.auth.authservice.dto.requestDto.ResendOtpRequestDto;
import com.quickbite.auth.authservice.dto.requestDto.VerifyOtpRequestDto;
import com.quickbite.auth.authservice.dto.responseDto.AuthResponseDto;
import com.quickbite.auth.authservice.dto.responseDto.OtpDispatchResponseDto;
import com.quickbite.auth.authservice.entity.AuthProvider;
import com.quickbite.auth.authservice.entity.PendingRegistration;
import com.quickbite.auth.authservice.entity.User;
import com.quickbite.auth.authservice.exception.BadRequestException;
import com.quickbite.auth.authservice.exception.InvalidEmailException;
import com.quickbite.auth.authservice.exception.InvalidPasswordException;
import com.quickbite.auth.authservice.exception.InvalidPhoneNumberException;
import com.quickbite.auth.authservice.exception.InvalidUserCredentialsException;
import com.quickbite.auth.authservice.exception.ResourceNotFoundException;
import com.quickbite.auth.authservice.exception.UserNotFoundException;
import com.quickbite.auth.authservice.mapper.AuthMapper;
import com.quickbite.auth.authservice.repository.PendingRegistrationRepository;
import com.quickbite.auth.authservice.repository.UserRepository;
import com.quickbite.auth.authservice.security.TokenBlacklist;
import com.quickbite.auth.authservice.service.AuthService;
import com.quickbite.auth.authservice.service.OtpNotificationService;
import com.quickbite.auth.authservice.util.JwtUtil;
import com.quickbite.auth.authservice.util.ValidatorUtility;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final PendingRegistrationRepository pendingRegistrationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final TokenBlacklist tokenBlacklist;
    private final OtpNotificationService otpNotificationService;

    @Value("${otp.expiry-minutes:10}")
    private long otpExpiryMinutes;

    @Value("${otp.max-attempts:5}")
    private int otpMaxAttempts;

    public AuthServiceImpl(UserRepository userRepository,
                           PendingRegistrationRepository pendingRegistrationRepository,
                           PasswordEncoder passwordEncoder,
                           JwtUtil jwtUtil,
                           TokenBlacklist tokenBlacklist,
                           OtpNotificationService otpNotificationService) {
        this.userRepository = userRepository;
        this.pendingRegistrationRepository = pendingRegistrationRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.tokenBlacklist = tokenBlacklist;
        this.otpNotificationService = otpNotificationService;
    }

    @Override
    @Transactional
    public OtpDispatchResponseDto requestRegistrationOtp(RegisterRequestDto request)
            throws InvalidEmailException, InvalidPhoneNumberException, InvalidPasswordException {

        cleanupExpiredPendingRegistrations();
        validateRegistrationRequest(request);
        ensureNoActiveUserExists(request.getEmail(), request.getPhoneNumber());
        clearExistingPendingRegistrations(request.getEmail(), request.getPhoneNumber());

        String rawOtp = generateOtp();
        PendingRegistration pendingRegistration = buildPendingRegistration(request, rawOtp);

        pendingRegistrationRepository.save(pendingRegistration);
        try {
            otpNotificationService.sendSignupOtp(
                    pendingRegistration.getName(),
                    pendingRegistration.getEmail(),
                    rawOtp
            );
        } catch (RuntimeException ex) {
            pendingRegistrationRepository.delete(pendingRegistration);
            throw ex;
        }

        log.info("Registration OTP created. verificationId={} email={} phone={}",
                pendingRegistration.getVerificationId(),
                pendingRegistration.getEmail(),
                maskPhoneNumber(pendingRegistration.getPhoneNumber()));

        return buildOtpDispatchResponse(
                pendingRegistration.getVerificationId(),
                pendingRegistration.getEmail(),
                "Verification code sent to your email address."
        );
    }

    @Override
    @Transactional
    public AuthResponseDto verifyRegistrationOtp(VerifyOtpRequestDto request) {
        cleanupExpiredPendingRegistrations();

        PendingRegistration pendingRegistration = getPendingRegistrationOrThrow(request.getVerificationId());

        if (pendingRegistration.getExpiresAt().isBefore(LocalDateTime.now())) {
            pendingRegistrationRepository.delete(pendingRegistration);
            throw new BadRequestException("Verification code expired. Please request a new OTP.");
        }

        if (!passwordEncoder.matches(request.getOtp(), pendingRegistration.getOtpHash())) {
            int updatedAttempts = pendingRegistration.getFailedAttempts() + 1;
            pendingRegistration.setFailedAttempts(updatedAttempts);

            if (updatedAttempts >= otpMaxAttempts) {
                pendingRegistrationRepository.delete(pendingRegistration);
                throw new BadRequestException("Maximum OTP attempts exceeded. Please request a new OTP.");
            }

            pendingRegistrationRepository.save(pendingRegistration);
            throw new BadRequestException(
                    "Invalid verification code. " + (otpMaxAttempts - updatedAttempts) + " attempt(s) remaining."
            );
        }

        try {
            ensureNoActiveUserExists(pendingRegistration.getEmail(), pendingRegistration.getPhoneNumber());
        } catch (InvalidEmailException | InvalidPhoneNumberException ex) {
            throw new BadRequestException(ex.getMessage());
        }

        User user = new User(
                pendingRegistration.getName(),
                pendingRegistration.getEmail(),
                pendingRegistration.getPhoneNumber(),
                pendingRegistration.getPasswordHash(),
                pendingRegistration.getRole(),
                AuthProvider.LOCAL
        );
        user.setActive(true);

        User saved = userRepository.save(user);
        pendingRegistrationRepository.delete(pendingRegistration);

        String accessToken = jwtUtil.generateToken(saved.getId(), saved.getEmail(), saved.getRole().toString());
        log.info("Registered new user after OTP verification. userId={} email={} role={}",
                saved.getId(),
                saved.getEmail(),
                saved.getRole());

        return AuthMapper.userToAuthResponse(saved, accessToken);
    }

    @Override
    @Transactional
    public OtpDispatchResponseDto resendRegistrationOtp(ResendOtpRequestDto request) {
        cleanupExpiredPendingRegistrations();

        PendingRegistration pendingRegistration = getPendingRegistrationOrThrow(request.getVerificationId());

        if (pendingRegistration.getExpiresAt().isBefore(LocalDateTime.now())) {
            pendingRegistrationRepository.delete(pendingRegistration);
            throw new BadRequestException("Verification code expired. Please start signup again.");
        }

        String rawOtp = generateOtp();
        pendingRegistration.setOtpHash(passwordEncoder.encode(rawOtp));
        pendingRegistration.setExpiresAt(LocalDateTime.now().plusMinutes(otpExpiryMinutes));
        pendingRegistration.setFailedAttempts(0);

        pendingRegistrationRepository.save(pendingRegistration);
        otpNotificationService.sendSignupOtp(
                pendingRegistration.getName(),
                pendingRegistration.getEmail(),
                rawOtp
        );

        log.info("Registration OTP resent. verificationId={} email={}",
                pendingRegistration.getVerificationId(),
                pendingRegistration.getEmail());

        return buildOtpDispatchResponse(
                pendingRegistration.getVerificationId(),
                pendingRegistration.getEmail(),
                "A fresh verification code has been sent to your email address."
        );
    }

    @Override
    public AuthResponseDto login(LoginRequestDto request) throws Exception {
        if (request.getLoginType() == LoginType.EMAIL) {
            String email = request.getIdentifier();
            ValidatorUtility.validateEmail(email);

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UserNotFoundException("User not found for " + email));

            ensureUserCanLogin(user, request.getPassword());

            String accessToken = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole().toString());
            log.info("Login successful by email. userId={} email={}", user.getId(), user.getEmail());
            return AuthMapper.userToAuthResponse(user, accessToken);
        }

        if (request.getLoginType() == LoginType.PHONE) {
            String phoneNumber = request.getIdentifier();
            ValidatorUtility.validatePhoneNumber(phoneNumber);

            User user = userRepository.findByPhoneNumber(phoneNumber)
                    .orElseThrow(() -> new UserNotFoundException("User not found for " + phoneNumber));

            ensureUserCanLogin(user, request.getPassword());

            String accessToken = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole().toString());
            log.info("Login successful by phone. userId={} phone={}", user.getId(), phoneNumber);
            return AuthMapper.userToAuthResponse(user, accessToken);
        }

        throw new IllegalArgumentException("Invalid login type");
    }

    @Override
    public void logout(HttpServletRequest request) {
        String token = extractToken(request);
        if (token != null) {
            tokenBlacklist.add(token);
            log.info("Token blacklisted on logout");
        }
    }

    @Override
    public boolean validateToken(String token) {
        try {
            if (tokenBlacklist.isBlacklisted(token)) {
                return false;
            }

            String email = jwtUtil.extractEmail(token);
            boolean valid = jwtUtil.isTokenValid(token, email);

            if (!valid) {
                return false;
            }

            User user = userRepository.findByEmail(email).orElse(null);
            return user != null && user.isActive();
        } catch (Exception ex) {
            return false;
        }
    }

    @Override
    public AuthResponseDto refreshToken(String token) {
        if (!validateToken(token)) {
            throw new RuntimeException("Invalid or expired token");
        }

        String email = jwtUtil.extractEmail(token);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String newToken = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole().toString());
        log.info("Token refreshed for userId={}", user.getId());

        return AuthMapper.userToAuthResponse(user, newToken);
    }

    @Override
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Override
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Override
    @Transactional
    public User updateProfile(Long userId, RegisterRequestDto request) throws InvalidPhoneNumberException {
        User user = getUserById(userId);

        if (request.getName() != null && !request.getName().isBlank()) {
            user.setName(request.getName());
        }

        if (request.getPhoneNumber() != null && !request.getPhoneNumber().isBlank()) {
            ValidatorUtility.validatePhoneNumber(request.getPhoneNumber());

            Optional<User> existingByPhone = userRepository.findByPhoneNumber(request.getPhoneNumber());
            if (existingByPhone.isPresent() && existingByPhone.get().getId() != user.getId()) {
                throw new RuntimeException("Phone number already exists");
            }

            user.setPhoneNumber(request.getPhoneNumber());
        }

        User updated = userRepository.save(user);
        log.info("Profile updated. userId={}", updated.getId());
        return updated;
    }

    @Override
    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) throws InvalidPasswordException {
        User user = getUserById(userId);

        if (user.getAuthProvider() != AuthProvider.LOCAL) {
            throw new RuntimeException("Password change is not allowed for OAuth-only users");
        }

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("Invalid old password");
        }

        ValidatorUtility.validatePassword(newPassword);
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        log.info("Password updated. userId={}", userId);
    }

    @Override
    @Transactional
    public void deactivateAccount(Long userId) {
        User user = getUserById(userId);
        user.setActive(false);
        userRepository.save(user);

        log.info("Account deactivated. userId={}", userId);
    }

    private PendingRegistration buildPendingRegistration(RegisterRequestDto request, String rawOtp) {
        PendingRegistration pendingRegistration = new PendingRegistration();
        pendingRegistration.setVerificationId(UUID.randomUUID().toString());
        pendingRegistration.setName(request.getName().trim());
        pendingRegistration.setEmail(request.getEmail().trim());
        pendingRegistration.setPhoneNumber(request.getPhoneNumber().trim());
        pendingRegistration.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        pendingRegistration.setRole(request.getRole());
        pendingRegistration.setOtpHash(passwordEncoder.encode(rawOtp));
        pendingRegistration.setCreatedAt(LocalDateTime.now());
        pendingRegistration.setExpiresAt(LocalDateTime.now().plusMinutes(otpExpiryMinutes));
        pendingRegistration.setFailedAttempts(0);
        return pendingRegistration;
    }

    private PendingRegistration getPendingRegistrationOrThrow(String verificationId) {
        return pendingRegistrationRepository.findById(verificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Pending registration not found"));
    }

    private void cleanupExpiredPendingRegistrations() {
        pendingRegistrationRepository.deleteAllByExpiresAtBefore(LocalDateTime.now());
    }

    private void clearExistingPendingRegistrations(String email, String phoneNumber) {
        List<PendingRegistration> duplicates = pendingRegistrationRepository.findAllByEmailOrPhoneNumber(email, phoneNumber);
        if (!duplicates.isEmpty()) {
            pendingRegistrationRepository.deleteAll(duplicates);
        }
    }

    private void validateRegistrationRequest(RegisterRequestDto request)
            throws InvalidEmailException, InvalidPhoneNumberException, InvalidPasswordException {
        if (request == null) {
            throw new BadRequestException("Registration request cannot be empty");
        }

        if (!StringUtils.hasText(request.getName())) {
            throw new BadRequestException("Name is required");
        }

        if (request.getRole() == null) {
            throw new BadRequestException("Role is required");
        }

        ValidatorUtility.validateEmail(request.getEmail());

        if (!StringUtils.hasText(request.getPhoneNumber())) {
            throw new InvalidPhoneNumberException("Phone number is required");
        }
        ValidatorUtility.validatePhoneNumber(request.getPhoneNumber());
        ValidatorUtility.validatePassword(request.getPassword());
    }

    private void ensureNoActiveUserExists(String email, String phoneNumber)
            throws InvalidEmailException, InvalidPhoneNumberException {
        if (userRepository.existsByEmail(email)) {
            throw new InvalidEmailException("Email already exists: " + email);
        }

        if (userRepository.existsByPhoneNumber(phoneNumber)) {
            throw new InvalidPhoneNumberException("Phone number already exists: " + phoneNumber);
        }
    }

    private OtpDispatchResponseDto buildOtpDispatchResponse(
            String verificationId,
            String email,
            String message) {
        return new OtpDispatchResponseDto(
                verificationId,
                maskEmail(email),
                otpExpiryMinutes * 60,
                message
        );
    }

    private String generateOtp() {
        return String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
    }

    private String maskEmail(String email) {
        if (!StringUtils.hasText(email) || !email.contains("@")) {
            return "hidden";
        }

        String[] parts = email.split("@", 2);
        String localPart = parts[0];
        if (localPart.length() <= 2) {
            return localPart.charAt(0) + "***@" + parts[1];
        }

        return localPart.charAt(0) + "***" + localPart.charAt(localPart.length() - 1) + "@" + parts[1];
    }

    private String maskPhoneNumber(String phoneNumber) {
        if (!StringUtils.hasText(phoneNumber) || phoneNumber.length() < 4) {
            return "hidden";
        }
        return "******" + phoneNumber.substring(phoneNumber.length() - 4);
    }

    private void ensureUserCanLogin(User user, String rawPassword) throws InvalidUserCredentialsException {
        if (!user.isActive()) {
            throw new RuntimeException("Account is deactivated");
        }

        if (user.getAuthProvider() != AuthProvider.LOCAL) {
            throw new RuntimeException("Use OAuth login for this account");
        }

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new InvalidUserCredentialsException("Invalid password");
        }
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
