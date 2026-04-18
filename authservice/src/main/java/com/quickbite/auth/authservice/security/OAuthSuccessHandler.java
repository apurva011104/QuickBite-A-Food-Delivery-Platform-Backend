package com.quickbite.auth.authservice.security;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
public class OAuthSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {

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
                    newUser.setActive(true);
                    return userRepository.save(newUser);
                });

        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole().toString());

        String redirectUrl = frontendUrl + "/oauth-success"
                + "?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8)
                + "&name=" + URLEncoder.encode(user.getName(), StandardCharsets.UTF_8)
                + "&email=" + URLEncoder.encode(user.getEmail(), StandardCharsets.UTF_8);

        response.sendRedirect(redirectUrl);
    }
}