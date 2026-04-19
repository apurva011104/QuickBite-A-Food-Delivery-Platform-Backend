package com.quickbite.payment.paymentservice.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserPrincipal {
    private Long userId;
    private String email;
    private String role;
}