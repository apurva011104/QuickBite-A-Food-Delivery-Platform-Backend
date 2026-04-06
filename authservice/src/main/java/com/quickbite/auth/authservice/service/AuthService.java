package com.quickbite.auth.authservice.service;

import com.quickbite.auth.authservice.dto.requestDto.LoginRequestDto;
import com.quickbite.auth.authservice.dto.requestDto.RegisterRequestDto;
import com.quickbite.auth.authservice.dto.responseDto.AuthResponseDto;
import com.quickbite.auth.authservice.exception.InvalidEmailException;
import com.quickbite.auth.authservice.exception.InvalidPasswordException;
import com.quickbite.auth.authservice.exception.InvalidPhoneNumberException;

import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {
    
    AuthResponseDto register(RegisterRequestDto request) throws InvalidEmailException, InvalidPhoneNumberException, InvalidPasswordException;

    AuthResponseDto login(LoginRequestDto request) throws Exception;

    void logout(HttpServletRequest request);
}
