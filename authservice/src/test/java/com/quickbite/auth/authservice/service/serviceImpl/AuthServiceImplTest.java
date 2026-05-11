package com.quickbite.auth.authservice.service.serviceImpl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.quickbite.auth.authservice.dto.requestDto.LoginRequestDto;
import com.quickbite.auth.authservice.dto.requestDto.LoginType;
import com.quickbite.auth.authservice.dto.requestDto.RegisterRequestDto;
import com.quickbite.auth.authservice.dto.responseDto.AuthResponseDto;
import com.quickbite.auth.authservice.entity.AuthProvider;
import com.quickbite.auth.authservice.entity.Role;
import com.quickbite.auth.authservice.entity.User;
import com.quickbite.auth.authservice.exception.InvalidEmailException;
import com.quickbite.auth.authservice.exception.InvalidPasswordException;
import com.quickbite.auth.authservice.exception.InvalidPhoneNumberException;
import com.quickbite.auth.authservice.exception.InvalidUserCredentialsException;
import com.quickbite.auth.authservice.exception.UserNotFoundException;
import com.quickbite.auth.authservice.repository.UserRepository;
import com.quickbite.auth.authservice.security.TokenBlacklist;
import com.quickbite.auth.authservice.util.JwtUtil;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private TokenBlacklist tokenBlacklist;

    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterRequestDto registerRequest;
    private User localUser;

    @BeforeEach
    void setUp() {
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
    void registerShouldPersistEncodedPasswordAndReturnToken()
            throws InvalidEmailException, InvalidPhoneNumberException, InvalidPasswordException {
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(userRepository.existsByPhoneNumber(registerRequest.getPhoneNumber())).thenReturn(false);
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            savedUser.setId(10L);
            return savedUser;
        });
        when(jwtUtil.generateToken(10L, registerRequest.getEmail(), Role.CUSTOMER.toString())).thenReturn("jwt-token");

        AuthResponseDto response = authService.register(registerRequest);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertEquals("encoded-password", savedUser.getPassword());
        assertTrue(savedUser.isActive());
        assertEquals("jwt-token", response.getAccessToken());
        assertEquals(registerRequest.getEmail(), response.getEmail());
    }

    @Test
    void registerShouldRejectDuplicateEmail() {
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(true);

        assertThrows(InvalidEmailException.class, () -> authService.register(registerRequest));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void loginWithEmailShouldReturnJwtWhenCredentialsAreValid() throws Exception {
        LoginRequestDto loginRequest = new LoginRequestDto("alice@example.com", "Password1", LoginType.EMAIL);

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(localUser));
        when(passwordEncoder.matches("Password1", localUser.getPassword())).thenReturn(true);
        when(jwtUtil.generateToken(localUser.getId(), localUser.getEmail(), localUser.getRole().toString()))
                .thenReturn("jwt-token");

        AuthResponseDto response = authService.login(loginRequest);

        assertEquals("jwt-token", response.getAccessToken());
        assertEquals(localUser.getEmail(), response.getEmail());
    }

    @Test
    void loginWithPhoneShouldRejectInvalidPassword() {
        LoginRequestDto loginRequest = new LoginRequestDto("9876543210", "WrongPass1", LoginType.PHONE);

        when(userRepository.findByPhoneNumber("9876543210")).thenReturn(Optional.of(localUser));
        when(passwordEncoder.matches("WrongPass1", localUser.getPassword())).thenReturn(false);

        assertThrows(InvalidUserCredentialsException.class, () -> authService.login(loginRequest));
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
    void loginWithEmailShouldThrowWhenUserIsMissing() {
        LoginRequestDto loginRequest = new LoginRequestDto("missing@example.com", "Password1", LoginType.EMAIL);
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> authService.login(loginRequest));
    }
}
