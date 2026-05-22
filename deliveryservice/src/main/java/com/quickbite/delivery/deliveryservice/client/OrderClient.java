package com.quickbite.delivery.deliveryservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import com.quickbite.delivery.deliveryservice.client.dto.OrderSummaryResponseDto;

@FeignClient(name = "ORDERSERVICE")
public interface OrderClient {

    @GetMapping("/orders/{orderId}")
    OrderSummaryResponseDto getOrderById(@PathVariable("orderId") Long orderId,
                                         @RequestHeader("Authorization") String authorizationHeader);

    @PutMapping("/orders/{orderId}/assign-agent")
    void assignDeliveryAgent(@PathVariable("orderId") Long orderId,
                             @RequestParam("agentId") Long agentId,
                             @RequestHeader("Authorization") String authorizationHeader);

    @PutMapping("/orders/{orderId}/status")
    void updateOrderStatus(@PathVariable("orderId") Long orderId,
                           @RequestParam("status") String status,
                           @RequestHeader("Authorization") String authorizationHeader);
}
