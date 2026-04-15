package com.quickbite.order.orderservice.external.payment.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.quickbite.order.orderservice.dto.requestDto.PaymentRequestDto;
import com.quickbite.order.orderservice.dto.responseDto.PaymentResponseDto;

@Service
public class PaymentClient {

    @Autowired
    private RestTemplate restTemplate;

    private final String PAYMENT_URL = "http://localhost:8085"; // adjust port

    public PaymentResponseDto processPayment(PaymentRequestDto request, String token) {

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);

        HttpEntity<PaymentRequestDto> entity = new HttpEntity<>(request, headers);

        ResponseEntity<PaymentResponseDto> response =
                restTemplate.postForEntity(
                        PAYMENT_URL + "/payments",
                        entity,
                        PaymentResponseDto.class
                );

        return response.getBody();
    }
}