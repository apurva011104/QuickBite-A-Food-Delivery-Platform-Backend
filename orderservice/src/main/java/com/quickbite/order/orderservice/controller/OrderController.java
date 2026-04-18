package com.quickbite.order.orderservice.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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
import com.quickbite.order.orderservice.exception.UnauthorizedActionException;
import com.quickbite.order.orderservice.service.OrderService;
import com.quickbite.order.orderservice.util.JwtUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private JwtUtil jwtUtil;

    //PLACE ORDER
    @PostMapping
    public ResponseEntity<OrderResponseDto> placeOrder( @Valid @RequestBody OrderRequestDto request,
                                HttpServletRequest httpRequest) {

        String token = extractToken(httpRequest);
        String role = jwtUtil.extractRole(token);
        if (!"CUSTOMER".equals(role)) {
            throw new UnauthorizedActionException("Only customers can place orders");
        }
        return ResponseEntity.ok(orderService.placeOrder(request, token));
    }

    //GET ORDER BY ID
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponseDto> getOrderById(@PathVariable Long orderId) {

        return ResponseEntity.ok(orderService.getOrderById(orderId));
    }

    //GET ORDERS FOR LOGGED-IN CUSTOMER
    @GetMapping("/customer")
    public ResponseEntity<List<OrderResponseDto>> getOrdersByCustomer(HttpServletRequest httpRequest) {
        String token = extractToken(httpRequest);
        String role = jwtUtil.extractRole(token);
        if (!"CUSTOMER".equals(role)) {
            throw new UnauthorizedActionException("Only customers can view customer orders");
        }
        return ResponseEntity.ok(orderService.getOrdersByCustomer(token));
    }

    //GET ORDERS BY RESTAURANT
    @GetMapping("/restaurant/{restaurantId}")
    public ResponseEntity<List<OrderResponseDto>> getOrdersByRestaurant(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(orderService.getOrdersByRestaurant(restaurantId));
    }

    //GET ACTIVE ORDERS
    @GetMapping("/active")
    public ResponseEntity<List<OrderResponseDto>> getActiveOrders() {

        return ResponseEntity.ok(orderService.getActiveOrders());
    }

    //UPDATE ORDER STATUS
    @PutMapping("/{orderId}/status")
    public ResponseEntity<OrderResponseDto> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestParam OrderStatus status) {

        return ResponseEntity.ok(orderService.updateOrderStatus(orderId, status));
    }

    //ASSIGN DELIVERY AGENT
    @PutMapping("/{orderId}/assign-agent")
    public ResponseEntity<OrderResponseDto> assignDeliveryAgent(
            @PathVariable Long orderId,
            @RequestParam Long agentId) {

        return ResponseEntity.ok(orderService.assignDeliveryAgent(orderId, agentId));
    }

    //CANCEL ORDER
    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<OrderResponseDto> cancelOrder(
            @PathVariable Long orderId,
            HttpServletRequest httpRequest) {

        String token = extractToken(httpRequest);
        String role = jwtUtil.extractRole(token);
        if (!"CUSTOMER".equals(role)) {
            throw new UnauthorizedActionException("Only customers can cancel orders");
        }
        return ResponseEntity.ok(orderService.cancelOrder(orderId, token));
    }

    //REORDER
    @PostMapping("/{orderId}/reorder")
    public ResponseEntity<OrderResponseDto> reorder(@PathVariable Long orderId,
                                     HttpServletRequest httpRequest) {

        String token = extractToken(httpRequest);
        String role = jwtUtil.extractRole(token);
        if (!"CUSTOMER".equals(role)) {
            throw new UnauthorizedActionException("Only customers can reorder");
        }
        return ResponseEntity.ok(orderService.reorderFromHistory(orderId, token));
    }

    //GET ORDER COUNT
    @GetMapping("/count/{restaurantId}")
    public ResponseEntity<Long> getOrderCount(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(orderService.getOrderCountByRestaurant(restaurantId));
    }

    //HELPER METHOD
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        throw new RuntimeException("Missing or invalid Authorization header");
    }
}