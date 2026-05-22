package com.quickbite.delivery.deliveryservice.config;

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

import com.quickbite.delivery.deliveryservice.security.JwtAuthenticationFilter;

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
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**"
                        ).permitAll()

                        .requestMatchers(HttpMethod.POST, "/api/v1/agents/register").hasRole("AGENT")

                        .requestMatchers(HttpMethod.PUT, "/api/v1/agents/*/location").hasRole("AGENT")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/agents/*/availability").hasRole("AGENT")
                        .requestMatchers(HttpMethod.POST, "/api/v1/agents/accept-delivery").hasRole("AGENT")
                        .requestMatchers(HttpMethod.POST, "/api/v1/agents/reject-delivery").hasRole("AGENT")
                        .requestMatchers(HttpMethod.POST, "/api/v1/agents/pickup-delivery").hasRole("AGENT")
                        .requestMatchers(HttpMethod.POST, "/api/v1/agents/complete-delivery").hasRole("AGENT")
                        .requestMatchers(HttpMethod.GET, "/api/v1/agents/*/active-deliveries").hasAnyRole("AGENT", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/agents/orders/*/completion-otp").hasAnyRole("CUSTOMER", "ADMIN")

                        .requestMatchers(HttpMethod.PUT, "/api/v1/agents/*/verify").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/agents/pending").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/agents/verified").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/agents/assign-order").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/agents/available").hasAnyRole("ADMIN", "OWNER")

                        // temporary internal/business endpoints
                        .requestMatchers(HttpMethod.GET, "/api/v1/agents/nearby").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/agents/*/rating").permitAll()

                        .requestMatchers(HttpMethod.GET, "/api/v1/agents/*").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/agents/user/*").authenticated()

                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
