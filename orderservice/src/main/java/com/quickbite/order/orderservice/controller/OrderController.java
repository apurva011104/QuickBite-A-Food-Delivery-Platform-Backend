package com.quickbite.order.orderservice.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.quickbite.order.orderservice.dto.requestDto.OrderRequestDto;
import com.quickbite.order.orderservice.dto.responseDto.OrderResponseDto;
import com.quickbite.order.orderservice.entity.OrderStatus;
import com.quickbite.order.orderservice.security.UserPrincipal;
import com.quickbite.order.orderservice.service.OrderService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/orders")
@Validated
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponseDto> placeOrder(@Valid @RequestBody OrderRequestDto request,
                                                       Authentication authentication,
                                                       HttpServletRequest httpServletRequest) {
        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
        String token = extractToken(httpServletRequest);
        return ResponseEntity.ok(orderService.placeOrder(request, user, token));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponseDto> getOrderById(@PathVariable Long orderId,
                                                         Authentication authentication,
                                                         HttpServletRequest httpServletRequest) {
        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
        String token = extractToken(httpServletRequest);
        return ResponseEntity.ok(orderService.getOrderById(orderId, user, token));
    }

    @GetMapping("/customer")
    public ResponseEntity<List<OrderResponseDto>> getOrdersByCustomer(Authentication authentication,
                                                                      HttpServletRequest httpServletRequest) {
        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
        String token = extractToken(httpServletRequest);
        return ResponseEntity.ok(orderService.getOrdersByCustomer(user.getUserId(), token));
    }

    @GetMapping("/restaurant/{restaurantId}")
    public ResponseEntity<List<OrderResponseDto>> getOrdersByRestaurant(@PathVariable Long restaurantId,
                                                                        Authentication authentication,
                                                                        HttpServletRequest httpServletRequest) {
        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
        String token = extractToken(httpServletRequest);
        return ResponseEntity.ok(orderService.getOrdersByRestaurant(restaurantId, user, token));
    }

    @GetMapping("/active")
    public ResponseEntity<List<OrderResponseDto>> getActiveOrders() {
        return ResponseEntity.ok(orderService.getActiveOrders());
    }

    @PutMapping("/{orderId}/status")
    public ResponseEntity<OrderResponseDto> updateOrderStatus(@PathVariable Long orderId,
                                                              @RequestParam OrderStatus status,
                                                              Authentication authentication,
                                                              HttpServletRequest httpServletRequest) {
        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
        String token = extractToken(httpServletRequest);
        return ResponseEntity.ok(orderService.updateOrderStatus(orderId, status, user, token));
    }

    @PutMapping("/{orderId}/assign-agent")
    public ResponseEntity<OrderResponseDto> assignDeliveryAgent(@PathVariable Long orderId,
                                                                @RequestParam Long agentId) {
        return ResponseEntity.ok(orderService.assignDeliveryAgent(orderId, agentId));
    }

    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<OrderResponseDto> cancelOrder(@PathVariable Long orderId,
                                                        Authentication authentication,
                                                        HttpServletRequest httpServletRequest) {
        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
        String token = extractToken(httpServletRequest);
        return ResponseEntity.ok(orderService.cancelOrder(orderId, user, token));
    }

    @PostMapping("/{orderId}/reorder")
    public ResponseEntity<OrderResponseDto> reorder(@PathVariable Long orderId,
                                                    Authentication authentication,
                                                    HttpServletRequest httpServletRequest) {
        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
        String token = extractToken(httpServletRequest);
        return ResponseEntity.ok(orderService.reorderFromHistory(orderId, user, token));
    }

    @GetMapping("/count/{restaurantId}")
    public ResponseEntity<Long> getOrderCount(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(orderService.getOrderCountByRestaurant(restaurantId));
    }

    
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        throw new RuntimeException("Missing or invalid Authorization header");
    }
}
