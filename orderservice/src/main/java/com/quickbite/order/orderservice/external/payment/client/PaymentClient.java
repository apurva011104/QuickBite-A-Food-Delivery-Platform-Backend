package com.quickbite.order.orderservice.external.payment.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import com.quickbite.order.orderservice.dto.requestDto.PaymentRequestDto;
import com.quickbite.order.orderservice.dto.responseDto.PaymentResponseDto;

@FeignClient(name = "PAYMENTSERVICE")
public interface PaymentClient {

    @PostMapping("/payments")
    PaymentResponseDto processPayment(@RequestBody PaymentRequestDto request,
                                      @RequestHeader("Authorization") String authorizationHeader);

    @PostMapping("/payments/refund/{orderId}")
    PaymentResponseDto refundPayment(@PathVariable("orderId") Long orderId,
                                     @RequestHeader("Authorization") String authorizationHeader);
}