package com.quickbite.delivery.deliveryservice.service.serviceImpl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import com.quickbite.delivery.deliveryservice.entity.DeliveryAgent;
import com.quickbite.delivery.deliveryservice.exception.AgentNotAvailableException;
import com.quickbite.delivery.deliveryservice.exception.AgentNotVerifiedException;
import com.quickbite.delivery.deliveryservice.exception.BadRequestException;
import com.quickbite.delivery.deliveryservice.exception.ResourceNotFoundException;
import com.quickbite.delivery.deliveryservice.mapper.DeliveryAgentMapper;
import com.quickbite.delivery.deliveryservice.repository.DeliveryRepository;
import com.quickbite.delivery.deliveryservice.service.DeliveryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class DeliveryServiceImpl implements DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final DeliveryAgentMapper deliveryAgentMapper;

    @Override
    public DeliveryAgentResponseDto registerAgent(DeliveryAgentRequestDto requestDto) {
        log.info("Registering delivery agent for userId: {}", requestDto.getUserId());

        if (deliveryRepository.existsByUserId(requestDto.getUserId())) {
            log.warn("Registration failed. Agent already exists for userId: {}", requestDto.getUserId());
            throw new BadRequestException("Agent already registered for this user ID");
        }

        if (deliveryRepository.existsByPhone(requestDto.getPhone())) {
            log.warn("Registration failed. Phone already in use: {}", requestDto.getPhone());
            throw new BadRequestException("Phone number already in use");
        }

        if (deliveryRepository.existsByVehicleNumber(requestDto.getVehicleNumber())) {
            log.warn("Registration failed. Vehicle number already in use: {}", requestDto.getVehicleNumber());
            throw new BadRequestException("Vehicle number already in use");
        }

        DeliveryAgent agent = deliveryAgentMapper.toEntity(requestDto);
        DeliveryAgent savedAgent = deliveryRepository.save(agent);

        log.info("Delivery agent registered successfully with agentId: {}", savedAgent.getAgentId());
        return deliveryAgentMapper.toResponseDto(savedAgent);
    }

    @Override
    @Transactional(readOnly = true)
    public DeliveryAgentResponseDto getAgentById(Long agentId) {
        log.info("Fetching delivery agent by agentId: {}", agentId);

        DeliveryAgent agent = deliveryRepository.findByAgentId(agentId)
                .orElseThrow(() -> {
                    log.warn("Agent not found with agentId: {}", agentId);
                    return new ResourceNotFoundException("Agent not found with ID: " + agentId);
                });

        return deliveryAgentMapper.toResponseDto(agent);
    }

    @Override
    @Transactional(readOnly = true)
    public DeliveryAgentResponseDto getAgentByUserId(Long userId) {
        log.info("Fetching delivery agent by userId: {}", userId);

        DeliveryAgent agent = deliveryRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    log.warn("Agent not found with userId: {}", userId);
                    return new ResourceNotFoundException("Agent not found with user ID: " + userId);
                });

        return deliveryAgentMapper.toResponseDto(agent);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryAgentResponseDto> getAllAvailableAgents() {
        log.info("Fetching all available delivery agents");

        List<DeliveryAgentResponseDto> agents = deliveryRepository.findByAvailableTrue()
                .stream()
                .map(deliveryAgentMapper::toResponseDto)
                .toList();

        log.info("Found {} available delivery agents", agents.size());
        return agents;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryAgentResponseDto> getAllVerifiedAgents() {
        log.info("Fetching all verified delivery agents");

        List<DeliveryAgentResponseDto> agents = deliveryRepository.findByVerifiedTrue()
                .stream()
                .map(deliveryAgentMapper::toResponseDto)
                .toList();

        log.info("Found {} verified delivery agents", agents.size());
        return agents;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryAgentResponseDto> getNearbyAgents(BigDecimal latitude, BigDecimal longitude, BigDecimal radiusKm) {
        log.info("Finding nearby agents for latitude: {}, longitude: {}, radiusKm: {}", latitude, longitude, radiusKm);

        if (latitude == null || longitude == null || radiusKm == null) {
            log.warn("Nearby agent search failed due to missing latitude/longitude/radius");
            throw new BadRequestException("Latitude, longitude, and radius are required");
        }

        List<DeliveryAgent> candidates = deliveryRepository.findByAvailableTrueAndVerifiedTrue();
        List<DeliveryAgentResponseDto> nearbyAgents = new ArrayList<>();

        for (DeliveryAgent agent : candidates) {
            if (agent.getCurrentLatitude() == null || agent.getCurrentLongitude() == null) {
                continue;
            }

            double distance = haversineDistance(
                    latitude.doubleValue(),
                    longitude.doubleValue(),
                    agent.getCurrentLatitude().doubleValue(),
                    agent.getCurrentLongitude().doubleValue()
            );

            if (distance <= radiusKm.doubleValue()) {
                nearbyAgents.add(deliveryAgentMapper.toResponseDto(agent));
            }
        }

        log.info("Found {} nearby agents within {} km", nearbyAgents.size(), radiusKm);
        return nearbyAgents;
    }

    @Override
    public MessageResponseDto updateLocation(Long agentId, LocationUpdateRequestDto requestDto) {
        log.info("Updating location for agentId: {}", agentId);

        DeliveryAgent agent = deliveryRepository.findByAgentId(agentId)
                .orElseThrow(() -> {
                    log.warn("Location update failed. Agent not found with agentId: {}", agentId);
                    return new ResourceNotFoundException("Agent not found with ID: " + agentId);
                });

        agent.setCurrentLatitude(requestDto.getCurrentLatitude());
        agent.setCurrentLongitude(requestDto.getCurrentLongitude());

        deliveryRepository.save(agent);

        log.info("Location updated successfully for agentId: {}", agentId);
        return new MessageResponseDto("Agent location updated successfully");
    }

    @Override
    public MessageResponseDto setAvailability(Long agentId, AvailabilityUpdateRequestDto requestDto) {
        log.info("Updating availability for agentId: {} to {}", agentId, requestDto.getAvailable());

        DeliveryAgent agent = deliveryRepository.findByAgentId(agentId)
                .orElseThrow(() -> {
                    log.warn("Availability update failed. Agent not found with agentId: {}", agentId);
                    return new ResourceNotFoundException("Agent not found with ID: " + agentId);
                });

        if (!agent.isVerified() && Boolean.TRUE.equals(requestDto.getAvailable())) {
            log.warn("Unverified agent cannot be marked available. agentId: {}", agentId);
            throw new AgentNotVerifiedException("Unverified agent cannot be set as available");
        }

        agent.setAvailable(requestDto.getAvailable());
        deliveryRepository.save(agent);

        log.info("Availability updated successfully for agentId: {}", agentId);
        return new MessageResponseDto("Agent availability updated successfully");
    }

    @Override
    public MessageResponseDto verifyAgent(Long agentId, VerificationRequestDto requestDto) {
        log.info("Updating verification for agentId: {} to {}", agentId, requestDto.getVerified());

        DeliveryAgent agent = deliveryRepository.findByAgentId(agentId)
                .orElseThrow(() -> {
                    log.warn("Verification failed. Agent not found with agentId: {}", agentId);
                    return new ResourceNotFoundException("Agent not found with ID: " + agentId);
                });

        agent.setVerified(requestDto.getVerified());

        if (!Boolean.TRUE.equals(requestDto.getVerified())) {
            agent.setAvailable(false);
        }

        deliveryRepository.save(agent);

        log.info("Verification updated successfully for agentId: {}", agentId);
        return new MessageResponseDto("Agent verification status updated successfully");
    }

    @Override
    public MessageResponseDto updateRating(Long agentId, RatingUpdateRequestDto requestDto) {
        log.info("Updating rating for agentId: {} with new rating: {}", agentId, requestDto.getNewRating());

        DeliveryAgent agent = deliveryRepository.findByAgentId(agentId)
                .orElseThrow(() -> {
                    log.warn("Rating update failed. Agent not found with agentId: {}", agentId);
                    return new ResourceNotFoundException("Agent not found with ID: " + agentId);
                });

        BigDecimal currentAvg = agent.getAvgRating();
        int totalDeliveries = agent.getTotalDeliveries();

        BigDecimal totalRatingSum = currentAvg.multiply(BigDecimal.valueOf(totalDeliveries));
        totalRatingSum = totalRatingSum.add(BigDecimal.valueOf(requestDto.getNewRating()));

        int updatedDeliveries = totalDeliveries + 1;
        BigDecimal updatedAvg = totalRatingSum.divide(BigDecimal.valueOf(updatedDeliveries), 2, RoundingMode.HALF_UP);

        agent.setTotalDeliveries(updatedDeliveries);
        agent.setAvgRating(updatedAvg);

        deliveryRepository.save(agent);

        log.info("Rating updated successfully for agentId: {}. New avgRating: {}", agentId, updatedAvg);
        return new MessageResponseDto("Agent rating updated successfully");
    }

    @Override
    public MessageResponseDto assignOrder(AssignOrderRequestDto requestDto) {
        log.info("Assigning orderId: {} to agentId: {}", requestDto.getOrderId(), requestDto.getAgentId());

        DeliveryAgent agent = deliveryRepository.findByAgentId(requestDto.getAgentId())
                .orElseThrow(() -> {
                    log.warn("Order assignment failed. Agent not found with agentId: {}", requestDto.getAgentId());
                    return new ResourceNotFoundException("Agent not found with ID: " + requestDto.getAgentId());
                });

        if (!agent.isVerified()) {
            log.warn("Order assignment failed. Agent not verified. agentId: {}", requestDto.getAgentId());
            throw new AgentNotVerifiedException("Agent is not verified");
        }

        if (!agent.isAvailable()) {
            log.warn("Order assignment failed. Agent not available. agentId: {}", requestDto.getAgentId());
            throw new AgentNotAvailableException("Agent is not available");
        }

        log.info("Order {} assigned successfully to agent {}", requestDto.getOrderId(), requestDto.getAgentId());
        return new MessageResponseDto(
                "Order " + requestDto.getOrderId() + " assigned to agent " + requestDto.getAgentId() + " successfully"
        );
    }

    @Override
    public MessageResponseDto completeDelivery(CompleteDeliveryRequestDto requestDto) {
        log.info("Completing delivery for orderId: {} by agentId: {}", requestDto.getOrderId(), requestDto.getAgentId());

        DeliveryAgent agent = deliveryRepository.findByAgentId(requestDto.getAgentId())
                .orElseThrow(() -> {
                    log.warn("Complete delivery failed. Agent not found with agentId: {}", requestDto.getAgentId());
                    return new ResourceNotFoundException("Agent not found with ID: " + requestDto.getAgentId());
                });

        if (!agent.isVerified()) {
            log.warn("Complete delivery failed. Agent not verified. agentId: {}", requestDto.getAgentId());
            throw new AgentNotVerifiedException("Agent is not verified");
        }

        log.info("Delivery completed for orderId: {} by agentId: {}", requestDto.getOrderId(), requestDto.getAgentId());
        return new MessageResponseDto(
                "Order " + requestDto.getOrderId() + " marked as delivered by agent " + requestDto.getAgentId()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActiveDeliveryResponseDto> getActiveDeliveries(Long agentId) {
        log.info("Fetching active deliveries for agentId: {}", agentId);

        deliveryRepository.findByAgentId(agentId)
                .orElseThrow(() -> {
                    log.warn("Active deliveries fetch failed. Agent not found with agentId: {}", agentId);
                    return new ResourceNotFoundException("Agent not found with ID: " + agentId);
                });

        log.info("Returning active deliveries for agentId: {}", agentId);
        return List.of();
    }

    private double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final int EARTH_RADIUS_KM = 6371;

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }
}