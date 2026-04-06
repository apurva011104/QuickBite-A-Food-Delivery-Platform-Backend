package com.quickbite.auth.authservice.security;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.quickbite.auth.authservice.entity.AuthProvider;
import com.quickbite.auth.authservice.entity.Role;
import com.quickbite.auth.authservice.entity.User;
import com.quickbite.auth.authservice.repository.UserRepository;
import com.quickbite.auth.authservice.util.JwtUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class OAuthSuccessHandler extends SimpleUrlAuthenticationSuccessHandler{

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response, 
                                        Authentication authentication) 
                                        throws IOException, ServletException{
                                        
        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();
                                        
        String email = oauthUser.getAttribute("email");
        String name = oauthUser.getAttribute("name");
                                        
        User user = userRepository.findByEmail(email)
                                .orElseGet(() -> {
                                    User newUser = new User();
                                    newUser.setEmail(email);
                                    newUser.setName(name);
                                    newUser.setAuthProvider(AuthProvider.GOOGLE);
                                    newUser.setRole(Role.CUSTOMER);
                                    return userRepository.save(newUser);
                                });

        String token = jwtUtil.generateToken(email, user.getRole().toString());
    
        String backendRedirectUrl = "http://localhost:8080/dashboard"
                + "?token=" + token
                + "&name=" + name
                + "&email=" + email;

        /* 
        String frontendRedirectUrl = "http://localhost:4200/dashboard"
                + "?token=" + token
                + "&name=" + name
                + "&email=" + email;
        */
    
        response.sendRedirect("http://localhost:8080/oauth-success?token=" + token);
    }

    
}
