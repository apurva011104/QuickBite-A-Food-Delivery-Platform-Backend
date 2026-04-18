package com.quickbite.order.orderservice.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
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
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                        // CUSTOMER
                        .requestMatchers(HttpMethod.POST, "/orders").hasAuthority("CUSTOMER")
                        .requestMatchers(HttpMethod.GET, "/orders/customer").hasAuthority("CUSTOMER")
                        .requestMatchers(HttpMethod.PUT, "/orders/*/cancel").hasAuthority("CUSTOMER")
                        .requestMatchers(HttpMethod.POST, "/orders/*/reorder").hasAuthority("CUSTOMER")

                        // OWNER / ADMIN
                        .requestMatchers(HttpMethod.GET, "/orders/restaurant/*").hasAnyAuthority("OWNER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/orders/count/*").hasAnyAuthority("OWNER", "ADMIN")

                        // OWNER / ADMIN / DELIVERY_AGENT
                        .requestMatchers(HttpMethod.GET, "/orders/active").hasAnyAuthority("OWNER", "ADMIN", "DELIVERY_AGENT")
                        .requestMatchers(HttpMethod.PUT, "/orders/*/status").hasAnyAuthority("OWNER", "ADMIN", "DELIVERY_AGENT")

                        // ADMIN only
                        .requestMatchers(HttpMethod.PUT, "/orders/*/assign-agent").hasAuthority("ADMIN")

                        // temporarily restrict order-by-id to all business roles, then tighten in service layer
                        .requestMatchers(HttpMethod.GET, "/orders/*").hasAnyAuthority("CUSTOMER", "OWNER", "ADMIN", "DELIVERY_AGENT")

                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}