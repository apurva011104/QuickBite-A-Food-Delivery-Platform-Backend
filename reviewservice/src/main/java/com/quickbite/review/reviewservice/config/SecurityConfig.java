package com.quickbite.review.reviewservice.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.quickbite.review.reviewservice.security.JwtAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/reviews/avg-food/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/reviews/avg-delivery/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/reviews/avg-menu-item/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/reviews/menu-item/*/summary").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/reviews").hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/reviews/customer/**").hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/reviews/*/moderate").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/reviews").hasAnyRole("ADMIN", "OWNER")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
