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
import com.quickbite.delivery.deliveryservice.entity.ActiveDelivery;
import com.quickbite.delivery.deliveryservice.entity.DeliveryAgent;
import com.quickbite.delivery.deliveryservice.entity.DeliveryStatus;
import com.quickbite.delivery.deliveryservice.exception.AgentNotAvailableException;
import com.quickbite.delivery.deliveryservice.exception.AgentNotVerifiedException;
import com.quickbite.delivery.deliveryservice.exception.BadRequestException;
import com.quickbite.delivery.deliveryservice.exception.ResourceNotFoundException;
import com.quickbite.delivery.deliveryservice.exception.UnauthorizedActionException;
import com.quickbite.delivery.deliveryservice.mapper.DeliveryAgentMapper;
import com.quickbite.delivery.deliveryservice.repository.ActiveDeliveryRepository;
import com.quickbite.delivery.deliveryservice.repository.DeliveryRepository;
import com.quickbite.delivery.deliveryservice.security.UserPrincipal;
import com.quickbite.delivery.deliveryservice.service.DeliveryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class DeliveryServiceImpl implements DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final ActiveDeliveryRepository activeDeliveryRepository;
    private final DeliveryAgentMapper deliveryAgentMapper;

    @Override
    public DeliveryAgentResponseDto registerAgent(UserPrincipal currentUser, DeliveryAgentRequestDto requestDto) {
        log.info("Registering delivery agent for userId={}", currentUser.getUserId());

        if (deliveryRepository.existsByUserId(currentUser.getUserId())) {
            throw new BadRequestException("Agent already registered for this user ID");
        }

        if (deliveryRepository.existsByPhone(requestDto.getPhone())) {
            throw new BadRequestException("Phone number already in use");
        }

        if (deliveryRepository.existsByVehicleNumber(requestDto.getVehicleNumber())) {
            throw new BadRequestException("Vehicle number already in use");
        }

        DeliveryAgent agent = deliveryAgentMapper.toEntity(requestDto);
        agent.setUserId(currentUser.getUserId());
        agent.setAvailable(false);
        agent.setVerified(false);

        DeliveryAgent savedAgent = deliveryRepository.save(agent);

        log.info("Delivery agent registered successfully with agentId={}", savedAgent.getAgentId());
        return deliveryAgentMapper.toResponseDto(savedAgent);
    }

    @Override
    @Transactional(readOnly = true)
    public DeliveryAgentResponseDto getAgentById(Long agentId) {
        DeliveryAgent agent = deliveryRepository.findByAgentId(agentId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent not found with ID: " + agentId));
        return deliveryAgentMapper.toResponseDto(agent);
    }

    @Override
    @Transactional(readOnly = true)
    public DeliveryAgentResponseDto getAgentByUserId(Long userId) {
        DeliveryAgent agent = deliveryRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent not found with user ID: " + userId));
        return deliveryAgentMapper.toResponseDto(agent);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryAgentResponseDto> getAllAvailableAgents() {
        return deliveryRepository.findByAvailableTrue()
                .stream()
                .map(deliveryAgentMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryAgentResponseDto> getAllVerifiedAgents() {
        return deliveryRepository.findByVerifiedTrue()
                .stream()
                .map(deliveryAgentMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryAgentResponseDto> getNearbyAgents(BigDecimal latitude, BigDecimal longitude, BigDecimal radiusKm) {
        if (latitude == null || longitude == null || radiusKm == null) {
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

        return nearbyAgents;
    }

    @Override
    public MessageResponseDto updateLocation(Long agentId, UserPrincipal currentUser, LocationUpdateRequestDto requestDto) {
        DeliveryAgent agent = getAgentOrThrow(agentId);
        validateAgentOwnership(agent, currentUser);

        agent.setCurrentLatitude(requestDto.getCurrentLatitude());
        agent.setCurrentLongitude(requestDto.getCurrentLongitude());

        deliveryRepository.save(agent);

        log.info("Location updated for agentId={}", agentId);
        return new MessageResponseDto("Agent location updated successfully");
    }

    @Override
    public MessageResponseDto setAvailability(Long agentId, UserPrincipal currentUser, AvailabilityUpdateRequestDto requestDto) {
        DeliveryAgent agent = getAgentOrThrow(agentId);
        validateAgentOwnership(agent, currentUser);

        if (!agent.isVerified() && Boolean.TRUE.equals(requestDto.getAvailable())) {
            throw new AgentNotVerifiedException("Unverified agent cannot be set as available");
        }

        boolean hasActiveAssignments = !activeDeliveryRepository
                .findByAgentIdAndStatusNot(agentId, DeliveryStatus.DELIVERED)
                .isEmpty();

        if (hasActiveAssignments && Boolean.FALSE.equals(requestDto.getAvailable())) {
            throw new BadRequestException("Agent cannot go offline with active deliveries");
        }

        agent.setAvailable(requestDto.getAvailable());
        deliveryRepository.save(agent);

        log.info("Availability updated for agentId={} available={}", agentId, requestDto.getAvailable());
        return new MessageResponseDto("Agent availability updated successfully");
    }

    @Override
    public MessageResponseDto verifyAgent(Long agentId, VerificationRequestDto requestDto) {
        DeliveryAgent agent = getAgentOrThrow(agentId);

        agent.setVerified(requestDto.getVerified());

        if (!Boolean.TRUE.equals(requestDto.getVerified())) {
            agent.setAvailable(false);
        }

        deliveryRepository.save(agent);

        log.info("Verification updated for agentId={} verified={}", agentId, requestDto.getVerified());
        return new MessageResponseDto("Agent verification status updated successfully");
    }

    @Override
    public MessageResponseDto updateRating(Long agentId, RatingUpdateRequestDto requestDto) {
        DeliveryAgent agent = getAgentOrThrow(agentId);

        BigDecimal currentAvg = agent.getAvgRating() == null ? BigDecimal.ZERO : agent.getAvgRating();
        int ratingCount = agent.getRatingCount() == null ? 0 : agent.getRatingCount();

        BigDecimal totalRatingSum = currentAvg.multiply(BigDecimal.valueOf(ratingCount))
                .add(BigDecimal.valueOf(requestDto.getNewRating()));

        int updatedRatingCount = ratingCount + 1;
        BigDecimal updatedAvg = totalRatingSum.divide(
                BigDecimal.valueOf(updatedRatingCount),
                2,
                RoundingMode.HALF_UP
        );

        agent.setRatingCount(updatedRatingCount);
        agent.setAvgRating(updatedAvg);

        deliveryRepository.save(agent);

        log.info("Rating updated for agentId={} avgRating={} ratingCount={}", agentId, updatedAvg, updatedRatingCount);
        return new MessageResponseDto("Agent rating updated successfully");
    }

    @Override
    public MessageResponseDto assignOrder(AssignOrderRequestDto requestDto) {
        DeliveryAgent agent = getAgentOrThrow(requestDto.getAgentId());

        if (!agent.isVerified()) {
            throw new AgentNotVerifiedException("Agent is not verified");
        }

        if (!agent.isAvailable()) {
            throw new AgentNotAvailableException("Agent is not available");
        }

        if (activeDeliveryRepository.existsByOrderId(requestDto.getOrderId())) {
            throw new BadRequestException("Order is already assigned");
        }

        ActiveDelivery activeDelivery = new ActiveDelivery(
                requestDto.getOrderId(),
                requestDto.getAgentId(),
                DeliveryStatus.ASSIGNED
        );

        activeDeliveryRepository.save(activeDelivery);

        agent.setAvailable(false);
        deliveryRepository.save(agent);

        log.info("Order {} assigned to agent {}", requestDto.getOrderId(), requestDto.getAgentId());
        return new MessageResponseDto(
                "Order " + requestDto.getOrderId() + " assigned to agent " + requestDto.getAgentId() + " successfully"
        );
    }

    @Override
    public MessageResponseDto completeDelivery(UserPrincipal currentUser, CompleteDeliveryRequestDto requestDto) {
        DeliveryAgent agent = getAgentOrThrow(requestDto.getAgentId());
        validateAgentOwnership(agent, currentUser);

        ActiveDelivery activeDelivery = activeDeliveryRepository.findByOrderId(requestDto.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Active delivery not found for order ID: " + requestDto.getOrderId()));

        if (!activeDelivery.getAgentId().equals(requestDto.getAgentId())) {
            throw new UnauthorizedActionException("This order is not assigned to the given agent");
        }

        activeDelivery.setStatus(DeliveryStatus.DELIVERED);
        activeDeliveryRepository.save(activeDelivery);

        agent.setAvailable(true);
        agent.setTotalDeliveries(agent.getTotalDeliveries() + 1);
        deliveryRepository.save(agent);

        log.info("Delivery completed for orderId={} by agentId={}", requestDto.getOrderId(), requestDto.getAgentId());
        return new MessageResponseDto(
                "Order " + requestDto.getOrderId() + " marked as delivered by agent " + requestDto.getAgentId()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActiveDeliveryResponseDto> getActiveDeliveries(Long agentId, UserPrincipal currentUser) {
        DeliveryAgent agent = getAgentOrThrow(agentId);

        if ("AGENT".equals(currentUser.getRole())) {
            validateAgentOwnership(agent, currentUser);
        }

        return activeDeliveryRepository.findByAgentIdAndStatusNot(agentId, DeliveryStatus.DELIVERED)
                .stream()
                .map(delivery -> new ActiveDeliveryResponseDto(
                        delivery.getOrderId(),
                        delivery.getAgentId(),
                        delivery.getStatus().name()
                ))
                .toList();
    }

    private DeliveryAgent getAgentOrThrow(Long agentId) {
        return deliveryRepository.findByAgentId(agentId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent not found with ID: " + agentId));
    }

    private void validateAgentOwnership(DeliveryAgent agent, UserPrincipal currentUser) {
        if (!agent.getUserId().equals(currentUser.getUserId())) {
            throw new UnauthorizedActionException("You are not allowed to perform this action");
        }
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