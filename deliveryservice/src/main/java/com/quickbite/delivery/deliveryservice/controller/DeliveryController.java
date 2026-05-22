package com.quickbite.delivery.deliveryservice.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.quickbite.delivery.deliveryservice.dto.requestDto.*;
import com.quickbite.delivery.deliveryservice.dto.responseDto.*;
import com.quickbite.delivery.deliveryservice.exception.BadRequestException;
import com.quickbite.delivery.deliveryservice.security.UserPrincipal;
import com.quickbite.delivery.deliveryservice.service.DeliveryService;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/agents")
@RequiredArgsConstructor
@Validated
@Slf4j
public class DeliveryController {

    private final DeliveryService deliveryService;

    @PostMapping("/register")
    public ResponseEntity<DeliveryAgentResponseDto> registerAgent(
            @Valid @RequestBody DeliveryAgentRequestDto requestDto,
            Authentication authentication) {
        UserPrincipal currentUser = (UserPrincipal) authentication.getPrincipal();
        log.info("API HIT - Register agent for userId: {}", currentUser.getUserId());
        return new ResponseEntity<>(deliveryService.registerAgent(currentUser, requestDto), HttpStatus.CREATED);
    }

    @GetMapping("/{agentId}")
    public ResponseEntity<DeliveryAgentResponseDto> getAgentById(@PathVariable Long agentId) {
        log.info("API HIT - Get agent by agentId: {}", agentId);
        return ResponseEntity.ok(deliveryService.getAgentById(agentId));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<DeliveryAgentResponseDto> getAgentByUserId(@PathVariable Long userId) {
        log.info("API HIT - Get agent by userId: {}", userId);
        return ResponseEntity.ok(deliveryService.getAgentByUserId(userId));
    }

    @GetMapping("/available")
    public ResponseEntity<List<DeliveryAgentResponseDto>> getAllAvailableAgents() {
        log.info("API HIT - Get all available agents");
        return ResponseEntity.ok(deliveryService.getAllAvailableAgents());
    }

    @GetMapping("/verified")
    public ResponseEntity<List<DeliveryAgentResponseDto>> getAllVerifiedAgents() {
        log.info("API HIT - Get all verified agents");
        return ResponseEntity.ok(deliveryService.getAllVerifiedAgents());
    }

    @GetMapping("/nearby")
    public ResponseEntity<List<DeliveryAgentResponseDto>> getNearbyAgents(
            @RequestParam BigDecimal latitude,
            @RequestParam BigDecimal longitude,
            @RequestParam BigDecimal radiusKm) {
        log.info("API HIT - Get nearby agents");
        return ResponseEntity.ok(deliveryService.getNearbyAgents(latitude, longitude, radiusKm));
    }

    @PutMapping("/{agentId}/location")
    public ResponseEntity<MessageResponseDto> updateLocation(
            @PathVariable Long agentId,
            @Valid @RequestBody LocationUpdateRequestDto requestDto,
            Authentication authentication) {
        UserPrincipal currentUser = (UserPrincipal) authentication.getPrincipal();
        log.info("API HIT - Update location for agentId: {}", agentId);
        return ResponseEntity.ok(deliveryService.updateLocation(agentId, currentUser, requestDto));
    }

    @PutMapping("/{agentId}/availability")
    public ResponseEntity<MessageResponseDto> setAvailability(
            @PathVariable Long agentId,
            @Valid @RequestBody AvailabilityUpdateRequestDto requestDto,
            Authentication authentication) {
        UserPrincipal currentUser = (UserPrincipal) authentication.getPrincipal();
        log.info("API HIT - Update availability for agentId: {}", agentId);
        return ResponseEntity.ok(deliveryService.setAvailability(agentId, currentUser, requestDto));
    }

    @PutMapping("/{agentId}/verify")
    public ResponseEntity<MessageResponseDto> verifyAgent(
            @PathVariable Long agentId,
            @Valid @RequestBody VerificationRequestDto requestDto) {
        log.info("API HIT - Verify agentId: {}", agentId);
        return ResponseEntity.ok(deliveryService.verifyAgent(agentId, requestDto));
    }

    @PutMapping("/{agentId}/rating")
    public ResponseEntity<MessageResponseDto> updateRating(
            @PathVariable Long agentId,
            @Valid @RequestBody RatingUpdateRequestDto requestDto) {
        log.info("API HIT - Update rating for agentId: {}", agentId);
        return ResponseEntity.ok(deliveryService.updateRating(agentId, requestDto));
    }

    @PostMapping("/assign-order")
    public ResponseEntity<MessageResponseDto> assignOrder(
            @Valid @RequestBody AssignOrderRequestDto requestDto,
            HttpServletRequest httpServletRequest) {
        log.info("API HIT - Assign orderId: {} to agentId: {}", requestDto.getOrderId(), requestDto.getAgentId());
        return ResponseEntity.ok(deliveryService.assignOrder(requestDto, extractAuthorizationHeader(httpServletRequest)));
    }

    @PostMapping("/pickup-delivery")
    public ResponseEntity<MessageResponseDto> pickupDelivery(
            @Valid @RequestBody PickupDeliveryRequestDto requestDto,
            Authentication authentication,
            HttpServletRequest httpServletRequest) {
        UserPrincipal currentUser = (UserPrincipal) authentication.getPrincipal();
        log.info("API HIT - Pickup delivery for orderId: {} by agentId: {}", requestDto.getOrderId(), requestDto.getAgentId());
        return ResponseEntity.ok(deliveryService.pickupDelivery(
                currentUser,
                requestDto,
                extractAuthorizationHeader(httpServletRequest)
        ));
    }

    @PostMapping("/accept-delivery")
    public ResponseEntity<MessageResponseDto> acceptDelivery(
            @Valid @RequestBody PickupDeliveryRequestDto requestDto,
            Authentication authentication) {
        UserPrincipal currentUser = (UserPrincipal) authentication.getPrincipal();
        log.info("API HIT - Accept delivery for orderId: {} by agentId: {}", requestDto.getOrderId(), requestDto.getAgentId());
        return ResponseEntity.ok(deliveryService.acceptDelivery(currentUser, requestDto));
    }

    @PostMapping("/reject-delivery")
    public ResponseEntity<MessageResponseDto> rejectDelivery(
            @Valid @RequestBody PickupDeliveryRequestDto requestDto,
            Authentication authentication) {
        UserPrincipal currentUser = (UserPrincipal) authentication.getPrincipal();
        log.info("API HIT - Reject delivery for orderId: {} by agentId: {}", requestDto.getOrderId(), requestDto.getAgentId());
        return ResponseEntity.ok(deliveryService.rejectDelivery(currentUser, requestDto));
    }

    @PostMapping("/complete-delivery")
    public ResponseEntity<MessageResponseDto> completeDelivery(
            @Valid @RequestBody CompleteDeliveryRequestDto requestDto,
            Authentication authentication,
            HttpServletRequest httpServletRequest) {
        UserPrincipal currentUser = (UserPrincipal) authentication.getPrincipal();
        log.info("API HIT - Complete delivery for orderId: {} by agentId: {}", requestDto.getOrderId(), requestDto.getAgentId());
        return ResponseEntity.ok(deliveryService.completeDelivery(
                currentUser,
                requestDto,
                extractAuthorizationHeader(httpServletRequest)
        ));
    }

    @GetMapping("/orders/{orderId}/completion-otp")
    public ResponseEntity<DeliveryCompletionOtpResponseDto> getCompletionOtp(@PathVariable Long orderId,
                                                                             Authentication authentication,
                                                                             HttpServletRequest httpServletRequest) {
        UserPrincipal currentUser = (UserPrincipal) authentication.getPrincipal();
        log.info("API HIT - Get completion OTP for orderId: {} by userId: {}", orderId, currentUser.getUserId());
        return ResponseEntity.ok(deliveryService.getCompletionOtp(
                orderId,
                currentUser,
                extractAuthorizationHeader(httpServletRequest)
        ));
    }

    @GetMapping("/{agentId}/active-deliveries")
    public ResponseEntity<List<ActiveDeliveryResponseDto>> getActiveDeliveries(@PathVariable Long agentId,
                                                                               Authentication authentication) {
        UserPrincipal currentUser = (UserPrincipal) authentication.getPrincipal();
        log.info("API HIT - Get active deliveries for agentId: {}", agentId);
        return ResponseEntity.ok(deliveryService.getActiveDeliveries(agentId, currentUser));
    }

    private String extractAuthorizationHeader(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header;
        }

        throw new BadRequestException("Missing or invalid Authorization header");
    }
}
