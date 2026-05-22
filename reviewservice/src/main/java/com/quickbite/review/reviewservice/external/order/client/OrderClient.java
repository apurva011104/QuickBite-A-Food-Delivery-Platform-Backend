package com.quickbite.review.reviewservice.external.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.quickbite.review.reviewservice.config.FeignAuthConfig;
import com.quickbite.review.reviewservice.external.order.dto.OrderSummaryResponseDto;

@FeignClient(name = "ORDERSERVICE", configuration = FeignAuthConfig.class)
public interface OrderClient {

    @GetMapping("/orders/{orderId}")
    OrderSummaryResponseDto getOrderById(@PathVariable("orderId") Long orderId);
}
