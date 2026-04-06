package com.quickbite.auth.authservice.service.serviceImpl;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private TokenBlacklist tokenBlacklist;

    @Override
    public AuthResponseDto register(RegisterRequestDto request) throws InvalidEmailException, InvalidPhoneNumberException, InvalidPasswordException{
        ValidatorUtility.validateEmail(request.getEmail());
        ValidatorUtility.validatePhoneNumber(request.getPhoneNumber());
        ValidatorUtility.validatePassword(request.getPassword());

        if(userRepository.existsByEmail(request.getEmail())){
            throw new InvalidEmailException("Email already exists: "+request.getEmail());
        }
        if(userRepository.existsByPhoneNumber(request.getPhoneNumber())){
            throw new InvalidPhoneNumberException("Phone number already exists: "+request.getPhoneNumber());
        }

        User user = AuthMapper.registerDtoToUser(request, AuthProvider.LOCAL);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);
        String accessToken = jwtUtil.generateToken(user.getEmail(), user.getRole().toString());
        return AuthMapper.userToAuthResponse(user, accessToken);

    }

    @Override
    public AuthResponseDto login(LoginRequestDto request) throws Exception{

        if(request.getLoginType()== LoginType.EMAIL){
            String email = request.getIdentifier();
            ValidatorUtility.validateEmail(email);
            Optional<User> user = userRepository.findByEmail(email);
            if(user.isEmpty()){
                throw new UserNotFoundException("User not found for "+email);
            }
            User u = user.get();
            if(!passwordEncoder.matches(request.getPassword(), u.getPassword())){
                throw new InvalidUserCredentialsException("Invalid password");
            }
            String accessToken = jwtUtil.generateToken(email, u.getRole().toString());
            return new AuthResponseDto(u.getName(), email, u.getPhoneNumber(), u.getRole(), accessToken );
        }

        else if(request.getLoginType()==LoginType.PHONE){
            String phoneNumber = request.getIdentifier();
            ValidatorUtility.validatePhoneNumber(phoneNumber);
            Optional<User> user = userRepository.findByPhoneNumber(phoneNumber);
            if(user.isEmpty()){
                throw new UserNotFoundException("User not found for "+phoneNumber);
            }
            User u = user.get();
            if(!passwordEncoder.matches(request.getPassword(), u.getPassword())){
                throw new InvalidUserCredentialsException("Invalid password");
            }
            String accessToken = jwtUtil.generateToken(u.getEmail(), u.getRole().toString());
            return new AuthResponseDto(u.getName(), u.getEmail(), phoneNumber, u.getRole(), accessToken);
        }
        
        throw new IllegalArgumentException("Invalid login type");
    }

    @Override
    public void logout(HttpServletRequest request) {
        String token = extractToken(request);

        if(token!=null){
            tokenBlacklist.add(token);
        }
    }

    private String extractToken(HttpServletRequest request){
        String header = request.getHeader("Authorization");

        if(header!=null && header.startsWith("Bearer ")){
            return header.substring(7);
        }
        return null;
    }
    
    
}
