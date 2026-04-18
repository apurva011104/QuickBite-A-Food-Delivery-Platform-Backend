package com.quickbite.auth.authservice.service.serviceImpl;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.quickbite.auth.authservice.dto.requestDto.LoginRequestDto;
import com.quickbite.auth.authservice.dto.requestDto.LoginType;
import com.quickbite.auth.authservice.dto.requestDto.RegisterRequestDto;
import com.quickbite.auth.authservice.dto.responseDto.AuthResponseDto;
import com.quickbite.auth.authservice.entity.AuthProvider;
import com.quickbite.auth.authservice.entity.User;
import com.quickbite.auth.authservice.exception.InvalidEmailException;
import com.quickbite.auth.authservice.exception.InvalidPasswordException;
import com.quickbite.auth.authservice.exception.InvalidPhoneNumberException;
import com.quickbite.auth.authservice.exception.InvalidUserCredentialsException;
import com.quickbite.auth.authservice.exception.UserNotFoundException;
import com.quickbite.auth.authservice.mapper.AuthMapper;
import com.quickbite.auth.authservice.repository.UserRepository;
import com.quickbite.auth.authservice.security.TokenBlacklist;
import com.quickbite.auth.authservice.service.AuthService;
import com.quickbite.auth.authservice.util.JwtUtil;
import com.quickbite.auth.authservice.util.ValidatorUtility;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final TokenBlacklist tokenBlacklist;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtUtil jwtUtil,
                           TokenBlacklist tokenBlacklist) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.tokenBlacklist = tokenBlacklist;
    }

    @Override
    @Transactional
    public AuthResponseDto register(RegisterRequestDto request)
            throws InvalidEmailException, InvalidPhoneNumberException, InvalidPasswordException {

        ValidatorUtility.validateEmail(request.getEmail());
        ValidatorUtility.validatePhoneNumber(request.getPhoneNumber());
        ValidatorUtility.validatePassword(request.getPassword());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new InvalidEmailException("Email already exists: " + request.getEmail());
        }

        if (request.getPhoneNumber() != null && !request.getPhoneNumber().isBlank()
                && userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new InvalidPhoneNumberException("Phone number already exists: " + request.getPhoneNumber());
        }

        User user = AuthMapper.registerDtoToUser(request, AuthProvider.LOCAL);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setActive(true);

        User saved = userRepository.save(user);
        String accessToken = jwtUtil.generateToken(saved.getId(), saved.getEmail(), saved.getRole().toString());

        log.info("Registered new user. userId={} email={} role={}", saved.getId(), saved.getEmail(), saved.getRole());
        return AuthMapper.userToAuthResponse(saved, accessToken);
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