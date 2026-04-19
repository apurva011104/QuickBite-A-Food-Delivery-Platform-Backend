package com.quickbite.order.orderservice.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.quickbite.order.orderservice.security.JwtAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                        .requestMatchers(HttpMethod.POST, "/orders").hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.GET, "/orders/customer").hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.PUT, "/orders/*/cancel").hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.POST, "/orders/*/reorder").hasRole("CUSTOMER")

                        .requestMatchers(HttpMethod.GET, "/orders/restaurant/*").hasAnyRole("OWNER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/orders/count/*").hasAnyRole("OWNER", "ADMIN")

                        .requestMatchers(HttpMethod.GET, "/orders/active").hasAnyRole("OWNER", "ADMIN", "AGENT")
                        .requestMatchers(HttpMethod.PUT, "/orders/*/status").hasAnyRole("OWNER", "ADMIN", "AGENT")

                        .requestMatchers(HttpMethod.PUT, "/orders/*/assign-agent").hasRole("ADMIN")

                        .requestMatchers(HttpMethod.GET, "/orders/*")
                        .hasAnyRole("CUSTOMER", "OWNER", "ADMIN", "AGENT")

                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}