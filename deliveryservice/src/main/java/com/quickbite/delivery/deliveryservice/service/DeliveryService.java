package com.quickbite.delivery.deliveryservice.service;

import java.math.BigDecimal;
import java.util.List;

import com.quickbite.delivery.deliveryservice.dto.requestDto.AssignOrderRequestDto;
import com.quickbite.delivery.deliveryservice.dto.requestDto.AvailabilityUpdateRequestDto;
import com.quickbite.delivery.deliveryservice.dto.requestDto.CompleteDeliveryRequestDto;
import com.quickbite.delivery.deliveryservice.dto.requestDto.DeliveryAgentRequestDto;
import com.quickbite.delivery.deliveryservice.dto.requestDto.LocationUpdateRequestDto;
import com.quickbite.delivery.deliveryservice.dto.requestDto.RatingUpdateRequestDto;
import com.quickbite.delivery.deliveryservice.dto.requestDto.VerificationRequestDto;
import com.quickbite.delivery.deliveryservice.dto.responseDto.ActiveDeliveryResponseDto;
import com.quickbite.delivery.deliveryservice.dto.responseDto.DeliveryAgentResponseDto;
import com.quickbite.delivery.deliveryservice.dto.responseDto.MessageResponseDto;

public interface DeliveryService {

    DeliveryAgentResponseDto registerAgent(DeliveryAgentRequestDto requestDto);

    DeliveryAgentResponseDto getAgentById(Long agentId);

    DeliveryAgentResponseDto getAgentByUserId(Long userId);

    List<DeliveryAgentResponseDto> getAllAvailableAgents();

    List<DeliveryAgentResponseDto> getAllVerifiedAgents();

    List<DeliveryAgentResponseDto> getNearbyAgents(BigDecimal latitude, BigDecimal longitude, BigDecimal radiusKm);

    MessageResponseDto updateLocation(Long agentId, LocationUpdateRequestDto requestDto);

    MessageResponseDto setAvailability(Long agentId, AvailabilityUpdateRequestDto requestDto);

    MessageResponseDto verifyAgent(Long agentId, VerificationRequestDto requestDto);

    MessageResponseDto updateRating(Long agentId, RatingUpdateRequestDto requestDto);

    MessageResponseDto assignOrder(AssignOrderRequestDto requestDto);

    MessageResponseDto completeDelivery(CompleteDeliveryRequestDto requestDto);

    List<ActiveDeliveryResponseDto> getActiveDeliveries(Long agentId);
}