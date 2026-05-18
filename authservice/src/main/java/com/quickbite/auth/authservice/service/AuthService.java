package com.quickbite.auth.authservice.service;

import com.quickbite.auth.authservice.dto.requestDto.LoginRequestDto;
import com.quickbite.auth.authservice.dto.requestDto.ForgotPasswordRequestDto;
import com.quickbite.auth.authservice.dto.requestDto.ResetPasswordWithOtpRequestDto;
import com.quickbite.auth.authservice.dto.requestDto.ResendOtpRequestDto;
import com.quickbite.auth.authservice.dto.requestDto.RegisterRequestDto;
import com.quickbite.auth.authservice.dto.requestDto.VerifyOtpRequestDto;
import com.quickbite.auth.authservice.dto.responseDto.AuthResponseDto;
import com.quickbite.auth.authservice.dto.responseDto.OtpDispatchResponseDto;
import com.quickbite.auth.authservice.entity.User;
import com.quickbite.auth.authservice.exception.InvalidEmailException;
import com.quickbite.auth.authservice.exception.InvalidPasswordException;
import com.quickbite.auth.authservice.exception.InvalidPhoneNumberException;

import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {
    
    OtpDispatchResponseDto requestRegistrationOtp(RegisterRequestDto request)
            throws InvalidEmailException, InvalidPhoneNumberException, InvalidPasswordException;

    AuthResponseDto verifyRegistrationOtp(VerifyOtpRequestDto request);

    OtpDispatchResponseDto resendRegistrationOtp(ResendOtpRequestDto request);

    OtpDispatchResponseDto requestLoginOtp(LoginRequestDto request) throws Exception;

    AuthResponseDto verifyLoginOtp(VerifyOtpRequestDto request);

    OtpDispatchResponseDto resendLoginOtp(ResendOtpRequestDto request);

    OtpDispatchResponseDto requestPasswordResetOtp(ForgotPasswordRequestDto request) throws Exception;

    String verifyPasswordResetOtp(ResetPasswordWithOtpRequestDto request) throws InvalidPasswordException;

    OtpDispatchResponseDto resendPasswordResetOtp(ResendOtpRequestDto request);

    void logout(HttpServletRequest request);

    boolean validateToken(String token);

    AuthResponseDto refreshToken(String token);

    User getUserByEmail(String email);

    User getUserById(Long id);

    User updateProfile(Long userId, RegisterRequestDto request) throws InvalidPhoneNumberException;

    void changePassword(Long userId, String oldPassword, String newPassword) throws InvalidPasswordException;

    void deactivateAccount(Long userId);
}
