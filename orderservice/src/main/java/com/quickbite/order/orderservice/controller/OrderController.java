package com.quickbite.order.orderservice.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.quickbite.order.orderservice.dto.requestDto.OrderRequestDto;
import com.quickbite.order.orderservice.dto.responseDto.OrderResponseDto;
import com.quickbite.order.orderservice.entity.OrderStatus;
import com.quickbite.order.orderservice.security.UserPrincipal;
import com.quickbite.order.orderservice.service.OrderService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/orders")
@Validated
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponseDto> placeOrder(@Valid @RequestBody OrderRequestDto request,
                                                       Authentication authentication) {
        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(orderService.placeOrder(request, user));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponseDto> getOrderById(@PathVariable Long orderId,
                                                         Authentication authentication) {
        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(orderService.getOrderById(orderId, user));
    }

    @GetMapping("/customer")
    public ResponseEntity<List<OrderResponseDto>> getOrdersByCustomer(Authentication authentication) {
        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(orderService.getOrdersByCustomer(user.getUserId()));
    }

    @GetMapping("/restaurant/{restaurantId}")
    public ResponseEntity<List<OrderResponseDto>> getOrdersByRestaurant(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(orderService.getOrdersByRestaurant(restaurantId));
    }

    @GetMapping("/active")
    public ResponseEntity<List<OrderResponseDto>> getActiveOrders() {
        return ResponseEntity.ok(orderService.getActiveOrders());
    }

    @PutMapping("/{orderId}/status")
    public ResponseEntity<OrderResponseDto> updateOrderStatus(@PathVariable Long orderId,
                                                              @RequestParam OrderStatus status) {
        return ResponseEntity.ok(orderService.updateOrderStatus(orderId, status));
    }

    @PutMapping("/{orderId}/assign-agent")
    public ResponseEntity<OrderResponseDto> assignDeliveryAgent(@PathVariable Long orderId,
                                                                @RequestParam Long agentId) {
        return ResponseEntity.ok(orderService.assignDeliveryAgent(orderId, agentId));
    }

    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<OrderResponseDto> cancelOrder(@PathVariable Long orderId,
                                                        Authentication authentication) {
        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(orderService.cancelOrder(orderId, user));
    }

    @PostMapping("/{orderId}/reorder")
    public ResponseEntity<OrderResponseDto> reorder(@PathVariable Long orderId,
                                                    Authentication authentication) {
        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(orderService.reorderFromHistory(orderId, user));
    }

    @GetMapping("/count/{restaurantId}")
    public ResponseEntity<Long> getOrderCount(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(orderService.getOrderCountByRestaurant(restaurantId));
    }
}