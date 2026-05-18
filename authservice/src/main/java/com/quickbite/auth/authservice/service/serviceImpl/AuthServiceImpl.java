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
import com.quickbite.auth.authservice.dto.requestDto.ForgotPasswordRequestDto;
import com.quickbite.auth.authservice.dto.requestDto.LoginType;
import com.quickbite.auth.authservice.dto.requestDto.ResetPasswordWithOtpRequestDto;
import com.quickbite.auth.authservice.dto.requestDto.RegisterRequestDto;
import com.quickbite.auth.authservice.dto.requestDto.ResendOtpRequestDto;
import com.quickbite.auth.authservice.dto.requestDto.VerifyOtpRequestDto;
import com.quickbite.auth.authservice.dto.responseDto.AuthResponseDto;
import com.quickbite.auth.authservice.dto.responseDto.OtpDispatchResponseDto;
import com.quickbite.auth.authservice.entity.AuthProvider;
import com.quickbite.auth.authservice.entity.PendingLogin;
import com.quickbite.auth.authservice.entity.PendingPasswordReset;
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
import com.quickbite.auth.authservice.repository.PendingLoginRepository;
import com.quickbite.auth.authservice.repository.PendingPasswordResetRepository;
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
    private final PendingLoginRepository pendingLoginRepository;
    private final PendingPasswordResetRepository pendingPasswordResetRepository;
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
                           PendingLoginRepository pendingLoginRepository,
                           PendingPasswordResetRepository pendingPasswordResetRepository,
                           PasswordEncoder passwordEncoder,
                           JwtUtil jwtUtil,
                           TokenBlacklist tokenBlacklist,
                           OtpNotificationService otpNotificationService) {
        this.userRepository = userRepository;
        this.pendingRegistrationRepository = pendingRegistrationRepository;
        this.pendingLoginRepository = pendingLoginRepository;
        this.pendingPasswordResetRepository = pendingPasswordResetRepository;
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
        otpNotificationService.sendRegistrationOtp(
                pendingRegistration.getName(),
                pendingRegistration.getEmail(),
                rawOtp
        );

        log.info("Registration OTP created. verificationId={} email={} phone={}",
                pendingRegistration.getVerificationId(),
                pendingRegistration.getEmail(),
                maskPhoneNumber(pendingRegistration.getPhoneNumber()));

        return buildOtpDispatchResponse(
                pendingRegistration.getVerificationId(),
                pendingRegistration.getEmail(),
                "Verification code is on its way to your email address. It may take a few moments to arrive."
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
        otpNotificationService.sendRegistrationOtp(
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
                "A fresh verification code is on its way to your email address."
        );
    }

    @Override
    @Transactional
    public OtpDispatchResponseDto requestLoginOtp(LoginRequestDto request) throws Exception {
        cleanupExpiredPendingLogins();

        User user = resolveUserForLogin(request);
        ensureUserCanLogin(user, request.getPassword());
        clearExistingPendingLogins(user.getId());

        String rawOtp = generateOtp();
        PendingLogin pendingLogin = buildPendingLogin(user, rawOtp);

        pendingLoginRepository.save(pendingLogin);
        otpNotificationService.sendLoginOtp(user.getName(), user.getEmail(), rawOtp);

        log.info("Login OTP created. verificationId={} userId={} email={}",
                pendingLogin.getVerificationId(),
                user.getId(),
                user.getEmail());

        return buildOtpDispatchResponse(
                pendingLogin.getVerificationId(),
                user.getEmail(),
                "Login verification code is on its way to your email address. It may take a few moments to arrive."
        );
    }

    @Override
    @Transactional
    public AuthResponseDto verifyLoginOtp(VerifyOtpRequestDto request) {
        cleanupExpiredPendingLogins();

        PendingLogin pendingLogin = getPendingLoginOrThrow(request.getVerificationId());

        if (pendingLogin.getExpiresAt().isBefore(LocalDateTime.now())) {
            pendingLoginRepository.delete(pendingLogin);
            throw new BadRequestException("Verification code expired. Please log in again.");
        }

        if (!passwordEncoder.matches(request.getOtp(), pendingLogin.getOtpHash())) {
            int updatedAttempts = pendingLogin.getFailedAttempts() + 1;
            pendingLogin.setFailedAttempts(updatedAttempts);

            if (updatedAttempts >= otpMaxAttempts) {
                pendingLoginRepository.delete(pendingLogin);
                throw new BadRequestException("Maximum OTP attempts exceeded. Please log in again.");
            }

            pendingLoginRepository.save(pendingLogin);
            throw new BadRequestException(
                    "Invalid verification code. " + (otpMaxAttempts - updatedAttempts) + " attempt(s) remaining."
            );
        }

        User user = userRepository.findById(pendingLogin.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        ensureOtpLoginAllowed(user);

        pendingLoginRepository.delete(pendingLogin);

        String accessToken = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole().toString());
        log.info("Login completed after OTP verification. userId={} email={}", user.getId(), user.getEmail());

        return AuthMapper.userToAuthResponse(user, accessToken);
    }

    @Override
    @Transactional
    public OtpDispatchResponseDto resendLoginOtp(ResendOtpRequestDto request) {
        cleanupExpiredPendingLogins();

        PendingLogin pendingLogin = getPendingLoginOrThrow(request.getVerificationId());

        if (pendingLogin.getExpiresAt().isBefore(LocalDateTime.now())) {
            pendingLoginRepository.delete(pendingLogin);
            throw new BadRequestException("Verification code expired. Please log in again.");
        }

        User user = userRepository.findById(pendingLogin.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        ensureOtpLoginAllowed(user);

        String rawOtp = generateOtp();
        pendingLogin.setOtpHash(passwordEncoder.encode(rawOtp));
        pendingLogin.setExpiresAt(LocalDateTime.now().plusMinutes(otpExpiryMinutes));
        pendingLogin.setFailedAttempts(0);

        pendingLoginRepository.save(pendingLogin);
        otpNotificationService.sendLoginOtp(user.getName(), user.getEmail(), rawOtp);

        log.info("Login OTP resent. verificationId={} userId={} email={}",
                pendingLogin.getVerificationId(),
                user.getId(),
                user.getEmail());

        return buildOtpDispatchResponse(
                pendingLogin.getVerificationId(),
                user.getEmail(),
                "A fresh login verification code is on its way to your email address."
        );
    }

    @Override
    @Transactional
    public OtpDispatchResponseDto requestPasswordResetOtp(ForgotPasswordRequestDto request) throws Exception {
        cleanupExpiredPendingPasswordResets();

        User user = resolveUserForIdentifier(request.getIdentifier(), request.getLoginType());
        ensurePasswordResetAllowed(user);
        clearExistingPendingPasswordResets(user.getId());

        String rawOtp = generateOtp();
        PendingPasswordReset pendingPasswordReset = buildPendingPasswordReset(user, rawOtp);

        pendingPasswordResetRepository.save(pendingPasswordReset);
        otpNotificationService.sendPasswordResetOtp(user.getName(), user.getEmail(), rawOtp);

        log.info("Password reset OTP created. verificationId={} userId={} email={}",
                pendingPasswordReset.getVerificationId(),
                user.getId(),
                user.getEmail());

        return buildOtpDispatchResponse(
                pendingPasswordReset.getVerificationId(),
                user.getEmail(),
                "Password reset code is on its way to your email address. It may take a few moments to arrive."
        );
    }

    @Override
    @Transactional
    public String verifyPasswordResetOtp(ResetPasswordWithOtpRequestDto request) throws InvalidPasswordException {
        cleanupExpiredPendingPasswordResets();

        PendingPasswordReset pendingPasswordReset = getPendingPasswordResetOrThrow(request.getVerificationId());

        if (pendingPasswordReset.getExpiresAt().isBefore(LocalDateTime.now())) {
            pendingPasswordResetRepository.delete(pendingPasswordReset);
            throw new BadRequestException("Verification code expired. Please request a new password reset OTP.");
        }

        if (!passwordEncoder.matches(request.getOtp(), pendingPasswordReset.getOtpHash())) {
            int updatedAttempts = pendingPasswordReset.getFailedAttempts() + 1;
            pendingPasswordReset.setFailedAttempts(updatedAttempts);

            if (updatedAttempts >= otpMaxAttempts) {
                pendingPasswordResetRepository.delete(pendingPasswordReset);
                throw new BadRequestException("Maximum OTP attempts exceeded. Please request a new password reset OTP.");
            }

            pendingPasswordResetRepository.save(pendingPasswordReset);
            throw new BadRequestException(
                    "Invalid verification code. " + (otpMaxAttempts - updatedAttempts) + " attempt(s) remaining."
            );
        }

        ValidatorUtility.validatePassword(request.getNewPassword());

        User user = userRepository.findById(pendingPasswordReset.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        ensurePasswordResetAllowed(user);

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        pendingPasswordResetRepository.delete(pendingPasswordReset);

        log.info("Password reset completed. userId={} email={}", user.getId(), user.getEmail());
        return "Password reset successfully";
    }

    @Override
    @Transactional
    public OtpDispatchResponseDto resendPasswordResetOtp(ResendOtpRequestDto request) {
        cleanupExpiredPendingPasswordResets();

        PendingPasswordReset pendingPasswordReset = getPendingPasswordResetOrThrow(request.getVerificationId());

        if (pendingPasswordReset.getExpiresAt().isBefore(LocalDateTime.now())) {
            pendingPasswordResetRepository.delete(pendingPasswordReset);
            throw new BadRequestException("Verification code expired. Please start the password reset flow again.");
        }

        User user = userRepository.findById(pendingPasswordReset.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        ensurePasswordResetAllowed(user);

        String rawOtp = generateOtp();
        pendingPasswordReset.setOtpHash(passwordEncoder.encode(rawOtp));
        pendingPasswordReset.setExpiresAt(LocalDateTime.now().plusMinutes(otpExpiryMinutes));
        pendingPasswordReset.setFailedAttempts(0);

        pendingPasswordResetRepository.save(pendingPasswordReset);
        otpNotificationService.sendPasswordResetOtp(user.getName(), user.getEmail(), rawOtp);

        log.info("Password reset OTP resent. verificationId={} userId={} email={}",
                pendingPasswordReset.getVerificationId(),
                user.getId(),
                user.getEmail());

        return buildOtpDispatchResponse(
                pendingPasswordReset.getVerificationId(),
                user.getEmail(),
                "A fresh password reset code is on its way to your email address."
        );
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

    private PendingLogin buildPendingLogin(User user, String rawOtp) {
        PendingLogin pendingLogin = new PendingLogin();
        pendingLogin.setVerificationId(UUID.randomUUID().toString());
        pendingLogin.setUserId(user.getId());
        pendingLogin.setEmail(user.getEmail());
        pendingLogin.setOtpHash(passwordEncoder.encode(rawOtp));
        pendingLogin.setCreatedAt(LocalDateTime.now());
        pendingLogin.setExpiresAt(LocalDateTime.now().plusMinutes(otpExpiryMinutes));
        pendingLogin.setFailedAttempts(0);
        return pendingLogin;
    }

    private PendingPasswordReset buildPendingPasswordReset(User user, String rawOtp) {
        PendingPasswordReset pendingPasswordReset = new PendingPasswordReset();
        pendingPasswordReset.setVerificationId(UUID.randomUUID().toString());
        pendingPasswordReset.setUserId(user.getId());
        pendingPasswordReset.setEmail(user.getEmail());
        pendingPasswordReset.setOtpHash(passwordEncoder.encode(rawOtp));
        pendingPasswordReset.setCreatedAt(LocalDateTime.now());
        pendingPasswordReset.setExpiresAt(LocalDateTime.now().plusMinutes(otpExpiryMinutes));
        pendingPasswordReset.setFailedAttempts(0);
        return pendingPasswordReset;
    }

    private PendingRegistration getPendingRegistrationOrThrow(String verificationId) {
        return pendingRegistrationRepository.findById(verificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Pending registration not found"));
    }

    private PendingLogin getPendingLoginOrThrow(String verificationId) {
        return pendingLoginRepository.findById(verificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Pending login not found"));
    }

    private PendingPasswordReset getPendingPasswordResetOrThrow(String verificationId) {
        return pendingPasswordResetRepository.findById(verificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Pending password reset not found"));
    }

    private void cleanupExpiredPendingRegistrations() {
        pendingRegistrationRepository.deleteAllByExpiresAtBefore(LocalDateTime.now());
    }

    private void cleanupExpiredPendingLogins() {
        pendingLoginRepository.deleteAllByExpiresAtBefore(LocalDateTime.now());
    }

    private void cleanupExpiredPendingPasswordResets() {
        pendingPasswordResetRepository.deleteAllByExpiresAtBefore(LocalDateTime.now());
    }

    private void clearExistingPendingRegistrations(String email, String phoneNumber) {
        List<PendingRegistration> duplicates = pendingRegistrationRepository.findAllByEmailOrPhoneNumber(email, phoneNumber);
        if (!duplicates.isEmpty()) {
            pendingRegistrationRepository.deleteAll(duplicates);
        }
    }

    private void clearExistingPendingLogins(Long userId) {
        List<PendingLogin> duplicates = pendingLoginRepository.findAllByUserId(userId);
        if (!duplicates.isEmpty()) {
            pendingLoginRepository.deleteAll(duplicates);
        }
    }

    private void clearExistingPendingPasswordResets(Long userId) {
        List<PendingPasswordReset> duplicates = pendingPasswordResetRepository.findAllByUserId(userId);
        if (!duplicates.isEmpty()) {
            pendingPasswordResetRepository.deleteAll(duplicates);
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

    private User resolveUserForLogin(LoginRequestDto request) throws Exception {
        if (request == null) {
            throw new BadRequestException("Login request cannot be empty");
        }

        if (!StringUtils.hasText(request.getPassword())) {
            throw new InvalidUserCredentialsException("Password is required");
        }

        return resolveUserForIdentifier(request.getIdentifier(), request.getLoginType());
    }

    private User resolveUserForIdentifier(String identifier, LoginType loginType) throws Exception {
        if (loginType == LoginType.EMAIL) {
            ValidatorUtility.validateEmail(identifier);

            return userRepository.findByEmail(identifier)
                    .orElseThrow(() -> new UserNotFoundException("User not found for " + identifier));
        }

        if (loginType == LoginType.PHONE) {
            ValidatorUtility.validatePhoneNumber(identifier);

            return userRepository.findByPhoneNumber(identifier)
                    .orElseThrow(() -> new UserNotFoundException("User not found for " + identifier));
        }

        throw new IllegalArgumentException("Invalid login type");
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
        ensureOtpLoginAllowed(user);

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new InvalidUserCredentialsException("Invalid password");
        }
    }

    private void ensureOtpLoginAllowed(User user) {
        if (!user.isActive()) {
            throw new RuntimeException("Account is deactivated");
        }

        if (user.getAuthProvider() != AuthProvider.LOCAL) {
            throw new RuntimeException("Use OAuth login for this account");
        }
    }

    private void ensurePasswordResetAllowed(User user) {
        ensureOtpLoginAllowed(user);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
