package com.quickbite.auth.authservice.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
import com.quickbite.auth.authservice.dto.requestDto.RegisterRequestDto;
import com.quickbite.auth.authservice.dto.responseDto.AuthResponseDto;
import com.quickbite.auth.authservice.entity.User;
import com.quickbite.auth.authservice.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;


@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public AuthResponseDto register(@RequestBody RegisterRequestDto registerRequestDto) throws Exception{
        AuthResponseDto dto = authService.register(registerRequestDto);
        System.out.println("Registered token: " + dto.getAccessToken());
        return dto;
    }

    
    @PostMapping("/login")
    public AuthResponseDto login(@RequestBody LoginRequestDto loginRequestDto) throws Exception{
        System.out.println("Logged in");
        return authService.login(loginRequestDto);
    }

    @GetMapping("/oauth-success")
    public String success(@RequestParam String token) {
        System.out.println(token);
        return token;
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request){
        authService.logout(request);
        System.out.println("Logged out. Token: "+ request.getHeader("Authorization"));
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
    public ResponseEntity<User> getProfile(Authentication auth) {
        String email = auth.getName();
        return ResponseEntity.ok(authService.getUserByEmail(email));
    }

    @PutMapping("/profile")
    public ResponseEntity<User> updateProfile(Authentication auth, @RequestBody RegisterRequestDto request) {
        String email = auth.getName();
        User user = authService.getUserByEmail(email);
        return ResponseEntity.ok(authService.updateProfile(user.getId(), request));
    }

    @PutMapping("/password")
    public ResponseEntity<String> changePassword(Authentication auth, @RequestBody ChangePasswordRequestDto request) {
        String email = auth.getName();
        User user = authService.getUserByEmail(email);
        authService.changePassword(user.getId(), request.getCurrentPassword(), request.getNewPassword());
        return ResponseEntity.ok("Password updated");
    }

    @DeleteMapping("/deactivate")
    public ResponseEntity<String> deactivate(Authentication auth) {
        String email = auth.getName();
        User user = authService.getUserByEmail(email);
        authService.deactivateAccount(user.getId());
        return ResponseEntity.ok("Account deactivated");
    }   
    
}
