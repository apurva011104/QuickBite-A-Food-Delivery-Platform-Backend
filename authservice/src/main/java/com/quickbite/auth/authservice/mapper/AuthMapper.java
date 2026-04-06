package com.quickbite.auth.authservice.mapper;

import com.quickbite.auth.authservice.dto.requestDto.RegisterRequestDto;
import com.quickbite.auth.authservice.dto.responseDto.AuthResponseDto;
import com.quickbite.auth.authservice.entity.AuthProvider;
import com.quickbite.auth.authservice.entity.User;

public class AuthMapper {
    
    public static User registerDtoToUser( RegisterRequestDto dto, AuthProvider provider){
        User user = new User(dto.getName(), 
                            dto.getEmail(), 
                            dto.getPhoneNumber(), 
                            dto.getPassword(), 
                            dto.getRole(),
                            provider);
        return user;
    }

    public static AuthResponseDto userToAuthResponse(User user, String accessToken){
        return new AuthResponseDto(user.getName(), 
                                    user.getEmail(), 
                                    user.getPhoneNumber(), 
                                    user.getRole(), 
                                    accessToken);
    }
}
