package com.quickbite.auth.authservice.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.quickbite.auth.authservice.dto.requestDto.ChangePasswordRequestDto;
import com.quickbite.auth.authservice.dto.requestDto.LoginRequestDto;
import com.quickbite.auth.authservice.dto.requestDto.ResendOtpRequestDto;
import com.quickbite.auth.authservice.dto.requestDto.RegisterRequestDto;
import com.quickbite.auth.authservice.dto.requestDto.VerifyOtpRequestDto;
import com.quickbite.auth.authservice.dto.responseDto.AuthResponseDto;
import com.quickbite.auth.authservice.dto.responseDto.OtpDispatchResponseDto;
import com.quickbite.auth.authservice.dto.responseDto.UserProfileResponseDto;
import com.quickbite.auth.authservice.entity.User;
import com.quickbite.auth.authservice.exception.InvalidPasswordException;
import com.quickbite.auth.authservice.exception.InvalidPhoneNumberException;
import com.quickbite.auth.authservice.mapper.AuthMapper;
import com.quickbite.auth.authservice.service.AuthService;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/auth")
@Validated
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<OtpDispatchResponseDto> requestRegistrationOtp(
            @Valid @RequestBody RegisterRequestDto request) throws Exception {
        OtpDispatchResponseDto response = authService.requestRegistrationOtp(request);
        log.info("Registration OTP issued. verificationId={} email={}", response.getVerificationId(), response.getMaskedEmail());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register/verify")
    public ResponseEntity<AuthResponseDto> verifyRegistrationOtp(
            @Valid @RequestBody VerifyOtpRequestDto request) {
        AuthResponseDto response = authService.verifyRegistrationOtp(request);
        log.info("User registered after OTP verification. email={} role={}", response.getEmail(), response.getRole());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register/resend")
    public ResponseEntity<OtpDispatchResponseDto> resendRegistrationOtp(
            @Valid @RequestBody ResendOtpRequestDto request) {
        OtpDispatchResponseDto response = authService.resendRegistrationOtp(request);
        log.info("Registration OTP resent. verificationId={} email={}", response.getVerificationId(), response.getMaskedEmail());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@RequestBody LoginRequestDto request) throws Exception {
        AuthResponseDto response = authService.login(request);
        log.info("User logged in successfully. email={}", response.getEmail());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/oauth-success")
    public ResponseEntity<String> success(@RequestParam String token) {
        return ResponseEntity.ok(token);
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request) {
        authService.logout(request);
        log.info("Logout request processed");
        return ResponseEntity.ok("Logged out successfully");
    }

    @PostMapping("/validate")
    public ResponseEntity<Boolean> validate(@RequestHeader("Authorization") String header) {
        String token = header.substring(7);
        return ResponseEntity.ok(authService.validateToken(token));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponseDto> refresh(@RequestHeader("Authorization") String header) {
        String token = header.substring(7);
        return ResponseEntity.ok(authService.refreshToken(token));
    }

    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponseDto> getProfile(Authentication auth) {
        String email = auth.getName();
        User user = authService.getUserByEmail(email);
        return ResponseEntity.ok(AuthMapper.userToProfileResponse(user));
    }

    @PutMapping("/profile")
    public ResponseEntity<UserProfileResponseDto> updateProfile(Authentication auth,
                                                                @RequestBody RegisterRequestDto request) throws InvalidPhoneNumberException {
        String email = auth.getName();
        User currentUser = authService.getUserByEmail(email);
        User updated = authService.updateProfile(currentUser.getId(), request);
        log.info("Profile updated for userId={}", updated.getId());
        return ResponseEntity.ok(AuthMapper.userToProfileResponse(updated));
    }

    @PutMapping("/password")
    public ResponseEntity<String> changePassword(Authentication auth,
                                                 @RequestBody ChangePasswordRequestDto request) throws InvalidPasswordException {
        String email = auth.getName();
        User user = authService.getUserByEmail(email);
        authService.changePassword(user.getId(), request.getCurrentPassword(), request.getNewPassword());
        log.info("Password changed for userId={}", user.getId());
        return ResponseEntity.ok("Password updated");
    }

    @DeleteMapping("/deactivate")
    public ResponseEntity<String> deactivate(Authentication auth) {
        String email = auth.getName();
        User user = authService.getUserByEmail(email);
        authService.deactivateAccount(user.getId());
        log.info("Account deactivated for userId={}", user.getId());
        return ResponseEntity.ok("Account deactivated");
    }
}
