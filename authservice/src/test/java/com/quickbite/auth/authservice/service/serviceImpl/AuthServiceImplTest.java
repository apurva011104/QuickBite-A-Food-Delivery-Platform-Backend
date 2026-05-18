package com.quickbite.auth.authservice.service.serviceImpl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

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
import com.quickbite.auth.authservice.entity.Role;
import com.quickbite.auth.authservice.entity.User;
import com.quickbite.auth.authservice.exception.BadRequestException;
import com.quickbite.auth.authservice.exception.InvalidEmailException;
import com.quickbite.auth.authservice.exception.InvalidPasswordException;
import com.quickbite.auth.authservice.exception.InvalidPhoneNumberException;
import com.quickbite.auth.authservice.exception.InvalidUserCredentialsException;
import com.quickbite.auth.authservice.exception.UserNotFoundException;
import com.quickbite.auth.authservice.repository.PendingLoginRepository;
import com.quickbite.auth.authservice.repository.PendingPasswordResetRepository;
import com.quickbite.auth.authservice.repository.PendingRegistrationRepository;
import com.quickbite.auth.authservice.repository.UserRepository;
import com.quickbite.auth.authservice.security.TokenBlacklist;
import com.quickbite.auth.authservice.service.OtpNotificationService;
import com.quickbite.auth.authservice.util.JwtUtil;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PendingRegistrationRepository pendingRegistrationRepository;

    @Mock
    private PendingLoginRepository pendingLoginRepository;

    @Mock
    private PendingPasswordResetRepository pendingPasswordResetRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private TokenBlacklist tokenBlacklist;

    @Mock
    private OtpNotificationService otpNotificationService;

    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterRequestDto registerRequest;
    private User localUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "otpExpiryMinutes", 10L);
        ReflectionTestUtils.setField(authService, "otpMaxAttempts", 5);

        registerRequest = new RegisterRequestDto(
                "Alice",
                "alice@example.com",
                "9876543210",
                "Password1",
                Role.CUSTOMER
        );

        localUser = new User(
                "Alice",
                "alice@example.com",
                "9876543210",
                "encoded-password",
                Role.CUSTOMER,
                AuthProvider.LOCAL
        );
        localUser.setId(10L);
        localUser.setActive(true);
    }

    @Test
    void requestRegistrationOtpShouldPersistPendingRegistrationAndDispatchCode()
            throws InvalidEmailException, InvalidPhoneNumberException, InvalidPasswordException {
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(userRepository.existsByPhoneNumber(registerRequest.getPhoneNumber())).thenReturn(false);
        when(pendingRegistrationRepository.findAllByEmailOrPhoneNumber(registerRequest.getEmail(), registerRequest.getPhoneNumber()))
                .thenReturn(List.of());
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password", "encoded-otp");
        when(pendingRegistrationRepository.save(any(PendingRegistration.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OtpDispatchResponseDto response = authService.requestRegistrationOtp(registerRequest);

        ArgumentCaptor<PendingRegistration> captor = ArgumentCaptor.forClass(PendingRegistration.class);
        verify(pendingRegistrationRepository).save(captor.capture());
        PendingRegistration savedPending = captor.getValue();

        assertEquals("encoded-password", savedPending.getPasswordHash());
        assertEquals("encoded-otp", savedPending.getOtpHash());
        assertTrue(savedPending.getExpiresAt().isAfter(LocalDateTime.now()));
        assertEquals(savedPending.getVerificationId(), response.getVerificationId());
        verify(otpNotificationService).sendRegistrationOtp(
                eq(registerRequest.getName()),
                eq(registerRequest.getEmail()),
                anyString()
        );
    }

    @Test
    void requestRegistrationOtpShouldRejectDuplicateEmail() {
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(true);

        assertThrows(InvalidEmailException.class, () -> authService.requestRegistrationOtp(registerRequest));
        verify(pendingRegistrationRepository, never()).save(any(PendingRegistration.class));
    }

    @Test
    void verifyRegistrationOtpShouldPersistUserAndReturnToken() {
        PendingRegistration pendingRegistration = buildPendingRegistration("verification-1", 0);

        when(pendingRegistrationRepository.findById("verification-1")).thenReturn(Optional.of(pendingRegistration));
        when(passwordEncoder.matches("123456", "encoded-otp")).thenReturn(true);
        when(userRepository.existsByEmail(pendingRegistration.getEmail())).thenReturn(false);
        when(userRepository.existsByPhoneNumber(pendingRegistration.getPhoneNumber())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            savedUser.setId(10L);
            return savedUser;
        });
        when(jwtUtil.generateToken(10L, pendingRegistration.getEmail(), pendingRegistration.getRole().toString()))
                .thenReturn("jwt-token");

        AuthResponseDto response = authService.verifyRegistrationOtp(new VerifyOtpRequestDto("verification-1", "123456"));

        assertEquals("jwt-token", response.getAccessToken());
        assertEquals(pendingRegistration.getEmail(), response.getEmail());
        verify(pendingRegistrationRepository).delete(pendingRegistration);
    }

    @Test
    void verifyRegistrationOtpShouldIncrementAttemptsWhenCodeIsWrong() {
        PendingRegistration pendingRegistration = buildPendingRegistration("verification-1", 0);

        when(pendingRegistrationRepository.findById("verification-1")).thenReturn(Optional.of(pendingRegistration));
        when(passwordEncoder.matches("111111", "encoded-otp")).thenReturn(false);
        when(pendingRegistrationRepository.save(any(PendingRegistration.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> authService.verifyRegistrationOtp(new VerifyOtpRequestDto("verification-1", "111111"))
        );

        assertTrue(exception.getMessage().contains("4 attempt(s) remaining"));
        assertEquals(1, pendingRegistration.getFailedAttempts());
        verify(pendingRegistrationRepository).save(pendingRegistration);
    }

    @Test
    void resendRegistrationOtpShouldResetAttemptsAndDispatchNewCode() {
        PendingRegistration pendingRegistration = buildPendingRegistration("verification-1", 3);

        when(pendingRegistrationRepository.findById("verification-1")).thenReturn(Optional.of(pendingRegistration));
        when(passwordEncoder.encode(anyString())).thenReturn("new-encoded-otp");
        when(pendingRegistrationRepository.save(any(PendingRegistration.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OtpDispatchResponseDto response = authService.resendRegistrationOtp(new ResendOtpRequestDto("verification-1"));

        assertEquals("verification-1", response.getVerificationId());
        assertEquals(0, pendingRegistration.getFailedAttempts());
        assertEquals("new-encoded-otp", pendingRegistration.getOtpHash());
        verify(otpNotificationService).sendRegistrationOtp(
                eq(pendingRegistration.getName()),
                eq(pendingRegistration.getEmail()),
                anyString()
        );
    }

    @Test
    void requestLoginOtpShouldPersistPendingLoginAndDispatchCode() throws Exception {
        LoginRequestDto loginRequest = new LoginRequestDto("alice@example.com", "Password1", LoginType.EMAIL);

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(localUser));
        when(passwordEncoder.matches("Password1", localUser.getPassword())).thenReturn(true);
        when(pendingLoginRepository.findAllByUserId(localUser.getId())).thenReturn(List.of());
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-login-otp");
        when(pendingLoginRepository.save(any(PendingLogin.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OtpDispatchResponseDto response = authService.requestLoginOtp(loginRequest);

        ArgumentCaptor<PendingLogin> captor = ArgumentCaptor.forClass(PendingLogin.class);
        verify(pendingLoginRepository).save(captor.capture());
        PendingLogin savedPending = captor.getValue();

        assertEquals(localUser.getId(), savedPending.getUserId());
        assertEquals("encoded-login-otp", savedPending.getOtpHash());
        assertEquals(savedPending.getVerificationId(), response.getVerificationId());
        verify(otpNotificationService).sendLoginOtp(
                eq(localUser.getName()),
                eq(localUser.getEmail()),
                anyString()
        );
    }

    @Test
    void verifyLoginOtpShouldReturnJwtWhenCodeIsValid() {
        PendingLogin pendingLogin = buildPendingLogin("login-verification-1", 0);

        when(pendingLoginRepository.findById("login-verification-1")).thenReturn(Optional.of(pendingLogin));
        when(passwordEncoder.matches("123456", "encoded-login-otp")).thenReturn(true);
        when(userRepository.findById(localUser.getId())).thenReturn(Optional.of(localUser));
        when(jwtUtil.generateToken(localUser.getId(), localUser.getEmail(), localUser.getRole().toString()))
                .thenReturn("jwt-token");

        AuthResponseDto response = authService.verifyLoginOtp(new VerifyOtpRequestDto("login-verification-1", "123456"));

        assertEquals("jwt-token", response.getAccessToken());
        assertEquals(localUser.getEmail(), response.getEmail());
        verify(pendingLoginRepository).delete(pendingLogin);
    }

    @Test
    void resendLoginOtpShouldResetAttemptsAndDispatchNewCode() {
        PendingLogin pendingLogin = buildPendingLogin("login-verification-1", 2);

        when(pendingLoginRepository.findById("login-verification-1")).thenReturn(Optional.of(pendingLogin));
        when(userRepository.findById(localUser.getId())).thenReturn(Optional.of(localUser));
        when(passwordEncoder.encode(anyString())).thenReturn("refreshed-login-otp");
        when(pendingLoginRepository.save(any(PendingLogin.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OtpDispatchResponseDto response = authService.resendLoginOtp(new ResendOtpRequestDto("login-verification-1"));

        assertEquals("login-verification-1", response.getVerificationId());
        assertEquals(0, pendingLogin.getFailedAttempts());
        assertEquals("refreshed-login-otp", pendingLogin.getOtpHash());
        verify(otpNotificationService).sendLoginOtp(
                eq(localUser.getName()),
                eq(localUser.getEmail()),
                anyString()
        );
    }

    @Test
    void requestPasswordResetOtpShouldPersistPendingResetAndDispatchCode() throws Exception {
        ForgotPasswordRequestDto request = new ForgotPasswordRequestDto("alice@example.com", LoginType.EMAIL);

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(localUser));
        when(pendingPasswordResetRepository.findAllByUserId(localUser.getId())).thenReturn(List.of());
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-reset-otp");
        when(pendingPasswordResetRepository.save(any(PendingPasswordReset.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OtpDispatchResponseDto response = authService.requestPasswordResetOtp(request);

        ArgumentCaptor<PendingPasswordReset> captor = ArgumentCaptor.forClass(PendingPasswordReset.class);
        verify(pendingPasswordResetRepository).save(captor.capture());
        PendingPasswordReset savedPending = captor.getValue();

        assertEquals(localUser.getId(), savedPending.getUserId());
        assertEquals("encoded-reset-otp", savedPending.getOtpHash());
        assertEquals(savedPending.getVerificationId(), response.getVerificationId());
        verify(otpNotificationService).sendPasswordResetOtp(
                eq(localUser.getName()),
                eq(localUser.getEmail()),
                anyString()
        );
    }

    @Test
    void verifyPasswordResetOtpShouldUpdatePasswordAndClearPendingReset() throws InvalidPasswordException {
        PendingPasswordReset pendingPasswordReset = buildPendingPasswordReset("reset-verification-1", 0);

        when(pendingPasswordResetRepository.findById("reset-verification-1")).thenReturn(Optional.of(pendingPasswordReset));
        when(passwordEncoder.matches("123456", "encoded-reset-otp")).thenReturn(true);
        when(userRepository.findById(localUser.getId())).thenReturn(Optional.of(localUser));
        when(passwordEncoder.encode("NewPassword1")).thenReturn("new-encoded-password");

        String response = authService.verifyPasswordResetOtp(
                new ResetPasswordWithOtpRequestDto("reset-verification-1", "123456", "NewPassword1")
        );

        assertEquals("Password reset successfully", response);
        assertEquals("new-encoded-password", localUser.getPassword());
        verify(userRepository).save(localUser);
        verify(pendingPasswordResetRepository).delete(pendingPasswordReset);
    }

    @Test
    void resendPasswordResetOtpShouldResetAttemptsAndDispatchNewCode() {
        PendingPasswordReset pendingPasswordReset = buildPendingPasswordReset("reset-verification-1", 2);

        when(pendingPasswordResetRepository.findById("reset-verification-1")).thenReturn(Optional.of(pendingPasswordReset));
        when(userRepository.findById(localUser.getId())).thenReturn(Optional.of(localUser));
        when(passwordEncoder.encode(anyString())).thenReturn("refreshed-reset-otp");
        when(pendingPasswordResetRepository.save(any(PendingPasswordReset.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OtpDispatchResponseDto response = authService.resendPasswordResetOtp(new ResendOtpRequestDto("reset-verification-1"));

        assertEquals("reset-verification-1", response.getVerificationId());
        assertEquals(0, pendingPasswordReset.getFailedAttempts());
        assertEquals("refreshed-reset-otp", pendingPasswordReset.getOtpHash());
        verify(otpNotificationService).sendPasswordResetOtp(
                eq(localUser.getName()),
                eq(localUser.getEmail()),
                anyString()
        );
    }

    @Test
    void loginWithPhoneShouldRejectInvalidPassword() {
        LoginRequestDto loginRequest = new LoginRequestDto("9876543210", "WrongPass1", LoginType.PHONE);

        when(userRepository.findByPhoneNumber("9876543210")).thenReturn(Optional.of(localUser));
        when(passwordEncoder.matches("WrongPass1", localUser.getPassword())).thenReturn(false);

        assertThrows(InvalidUserCredentialsException.class, () -> authService.requestLoginOtp(loginRequest));
    }

    @Test
    void validateTokenShouldReturnFalseForBlacklistedTokens() {
        when(tokenBlacklist.isBlacklisted("jwt-token")).thenReturn(true);

        assertFalse(authService.validateToken("jwt-token"));
        verify(jwtUtil, never()).extractEmail(anyString());
    }

    @Test
    void validateTokenShouldReturnTrueForActiveUserWithValidToken() {
        when(tokenBlacklist.isBlacklisted("jwt-token")).thenReturn(false);
        when(jwtUtil.extractEmail("jwt-token")).thenReturn(localUser.getEmail());
        when(jwtUtil.isTokenValid("jwt-token", localUser.getEmail())).thenReturn(true);
        when(userRepository.findByEmail(localUser.getEmail())).thenReturn(Optional.of(localUser));

        assertTrue(authService.validateToken("jwt-token"));
    }

    @Test
    void refreshTokenShouldThrowWhenIncomingTokenIsInvalid() {
        when(tokenBlacklist.isBlacklisted("jwt-token")).thenReturn(true);

        assertThrows(RuntimeException.class, () -> authService.refreshToken("jwt-token"));
        verify(jwtUtil, never()).generateToken(anyLong(), anyString(), anyString());
    }

    @Test
    void changePasswordShouldRejectOauthOnlyUsers() {
        User oauthUser = new User(
                "Alice",
                "alice@example.com",
                "9876543210",
                "unused",
                Role.CUSTOMER,
                AuthProvider.GOOGLE
        );
        oauthUser.setId(11L);

        when(userRepository.findById(11L)).thenReturn(Optional.of(oauthUser));

        assertThrows(RuntimeException.class, () -> authService.changePassword(11L, "old", "Password1"));
    }

    @Test
    void deactivateAccountShouldMarkUserInactive() {
        when(userRepository.findById(localUser.getId())).thenReturn(Optional.of(localUser));
        when(userRepository.save(localUser)).thenReturn(localUser);

        assertDoesNotThrow(() -> authService.deactivateAccount(localUser.getId()));

        assertFalse(localUser.isActive());
        verify(userRepository).save(localUser);
    }

    @Test
    void getUserByEmailShouldThrowWhenUserDoesNotExist() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> authService.getUserByEmail("missing@example.com"));
    }

    @Test
    void requestLoginOtpShouldThrowWhenUserIsMissing() {
        LoginRequestDto loginRequest = new LoginRequestDto("missing@example.com", "Password1", LoginType.EMAIL);
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> authService.requestLoginOtp(loginRequest));
    }

    private PendingRegistration buildPendingRegistration(String verificationId, int failedAttempts) {
        PendingRegistration pendingRegistration = new PendingRegistration();
        pendingRegistration.setVerificationId(verificationId);
        pendingRegistration.setName("Alice");
        pendingRegistration.setEmail("alice@example.com");
        pendingRegistration.setPhoneNumber("9876543210");
        pendingRegistration.setPasswordHash("encoded-password");
        pendingRegistration.setRole(Role.CUSTOMER);
        pendingRegistration.setOtpHash("encoded-otp");
        pendingRegistration.setCreatedAt(LocalDateTime.now());
        pendingRegistration.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        pendingRegistration.setFailedAttempts(failedAttempts);
        return pendingRegistration;
    }

    private PendingLogin buildPendingLogin(String verificationId, int failedAttempts) {
        PendingLogin pendingLogin = new PendingLogin();
        pendingLogin.setVerificationId(verificationId);
        pendingLogin.setUserId(localUser.getId());
        pendingLogin.setEmail(localUser.getEmail());
        pendingLogin.setOtpHash("encoded-login-otp");
        pendingLogin.setCreatedAt(LocalDateTime.now());
        pendingLogin.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        pendingLogin.setFailedAttempts(failedAttempts);
        return pendingLogin;
    }

    private PendingPasswordReset buildPendingPasswordReset(String verificationId, int failedAttempts) {
        PendingPasswordReset pendingPasswordReset = new PendingPasswordReset();
        pendingPasswordReset.setVerificationId(verificationId);
        pendingPasswordReset.setUserId(localUser.getId());
        pendingPasswordReset.setEmail(localUser.getEmail());
        pendingPasswordReset.setOtpHash("encoded-reset-otp");
        pendingPasswordReset.setCreatedAt(LocalDateTime.now());
        pendingPasswordReset.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        pendingPasswordReset.setFailedAttempts(failedAttempts);
        return pendingPasswordReset;
    }
}
