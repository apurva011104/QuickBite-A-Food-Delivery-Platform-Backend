package com.quickbite.delivery.deliveryservice.service.serviceImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.quickbite.delivery.deliveryservice.dto.requestDto.AssignOrderRequestDto;
import com.quickbite.delivery.deliveryservice.dto.requestDto.CompleteDeliveryRequestDto;
import com.quickbite.delivery.deliveryservice.dto.requestDto.LocationUpdateRequestDto;
import com.quickbite.delivery.deliveryservice.dto.requestDto.PickupDeliveryRequestDto;
import com.quickbite.delivery.deliveryservice.entity.ActiveDelivery;
import com.quickbite.delivery.deliveryservice.entity.DeliveryAgent;
import com.quickbite.delivery.deliveryservice.entity.DeliveryStatus;
import com.quickbite.delivery.deliveryservice.enums.VehicleType;
import com.quickbite.delivery.deliveryservice.event.NotificationEvent;
import com.quickbite.delivery.deliveryservice.exception.BadRequestException;
import com.quickbite.delivery.deliveryservice.exception.UnauthorizedActionException;
import com.quickbite.delivery.deliveryservice.mapper.DeliveryAgentMapper;
import com.quickbite.delivery.deliveryservice.repository.ActiveDeliveryRepository;
import com.quickbite.delivery.deliveryservice.repository.DeliveryRepository;
import com.quickbite.delivery.deliveryservice.security.UserPrincipal;
import com.quickbite.delivery.deliveryservice.service.NotificationEventPublisher;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceImplTest {

    @Mock
    private DeliveryRepository deliveryRepository;

    @Mock
    private ActiveDeliveryRepository activeDeliveryRepository;

    @Mock
    private NotificationEventPublisher notificationEventPublisher;

    private DeliveryServiceImpl deliveryService;
    private DeliveryAgent agent;
    private UserPrincipal currentUser;

    @BeforeEach
    void setUp() {
        deliveryService = new DeliveryServiceImpl();
        ReflectionTestUtils.setField(deliveryService, "deliveryRepository", deliveryRepository);
        ReflectionTestUtils.setField(deliveryService, "activeDeliveryRepository", activeDeliveryRepository);
        ReflectionTestUtils.setField(deliveryService, "deliveryAgentMapper", new DeliveryAgentMapper());
        ReflectionTestUtils.setField(deliveryService, "notificationEventPublisher", notificationEventPublisher);

        currentUser = new UserPrincipal(10L, "agent@quickbite.com", "AGENT");

        agent = new DeliveryAgent();
        agent.setAgentId(7L);
        agent.setUserId(10L);
        agent.setFullName("Delivery Pro");
        agent.setPhone("9999999999");
        agent.setVehicleType(VehicleType.BIKE);
        agent.setVehicleNumber("KA01AB1234");
        agent.setCurrentLatitude(BigDecimal.valueOf(12.9716));
        agent.setCurrentLongitude(BigDecimal.valueOf(77.5946));
        agent.setAvailable(true);
        agent.setVerified(true);
        agent.setAvgRating(BigDecimal.valueOf(4.50));
        agent.setTotalDeliveries(2);
        agent.setRatingCount(2);
    }

    @Test
    void assignOrderShouldPersistAssignmentAndSetAgentOffline() {
        AssignOrderRequestDto requestDto = new AssignOrderRequestDto();
        requestDto.setAgentId(7L);
        requestDto.setOrderId(101L);

        when(deliveryRepository.findByAgentId(7L)).thenReturn(Optional.of(agent));
        when(activeDeliveryRepository.existsByOrderId(101L)).thenReturn(false);
        when(activeDeliveryRepository.save(any(ActiveDelivery.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(deliveryRepository.save(any(DeliveryAgent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = deliveryService.assignOrder(requestDto);

        assertThat(response.getMessage()).contains("Order 101 assigned");
        assertThat(agent.isAvailable()).isFalse();

        ArgumentCaptor<ActiveDelivery> captor = ArgumentCaptor.forClass(ActiveDelivery.class);
        verify(activeDeliveryRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(DeliveryStatus.ASSIGNED);
        verify(notificationEventPublisher).publishDeliveryNotification(any(NotificationEvent.class));
    }

    @Test
    void pickupDeliveryShouldMoveAssignedDeliveryToPickedUp() {
        PickupDeliveryRequestDto requestDto = new PickupDeliveryRequestDto();
        requestDto.setAgentId(7L);
        requestDto.setOrderId(101L);

        ActiveDelivery activeDelivery = new ActiveDelivery(101L, 7L, DeliveryStatus.ASSIGNED);

        when(deliveryRepository.findByAgentId(7L)).thenReturn(Optional.of(agent));
        when(activeDeliveryRepository.findByOrderId(101L)).thenReturn(Optional.of(activeDelivery));
        when(activeDeliveryRepository.save(any(ActiveDelivery.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = deliveryService.pickupDelivery(currentUser, requestDto);

        assertThat(response.getMessage()).contains("picked up");
        assertThat(activeDelivery.getStatus()).isEqualTo(DeliveryStatus.PICKED_UP);
    }

    @Test
    void pickupDeliveryShouldRejectWrongAgent() {
        PickupDeliveryRequestDto requestDto = new PickupDeliveryRequestDto();
        requestDto.setAgentId(7L);
        requestDto.setOrderId(101L);

        DeliveryAgent otherAgent = new DeliveryAgent();
        otherAgent.setAgentId(8L);
        otherAgent.setUserId(99L);

        when(deliveryRepository.findByAgentId(7L)).thenReturn(Optional.of(otherAgent));

        assertThatThrownBy(() -> deliveryService.pickupDelivery(currentUser, requestDto))
                .isInstanceOf(UnauthorizedActionException.class)
                .hasMessage("You are not allowed to perform this action");
    }

    @Test
    void completeDeliveryShouldRejectWhenOrderNotPickedUpYet() {
        CompleteDeliveryRequestDto requestDto = new CompleteDeliveryRequestDto();
        requestDto.setAgentId(7L);
        requestDto.setOrderId(101L);

        ActiveDelivery activeDelivery = new ActiveDelivery(101L, 7L, DeliveryStatus.ASSIGNED);

        when(deliveryRepository.findByAgentId(7L)).thenReturn(Optional.of(agent));
        when(activeDeliveryRepository.findByOrderId(101L)).thenReturn(Optional.of(activeDelivery));

        assertThatThrownBy(() -> deliveryService.completeDelivery(currentUser, requestDto))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Delivery must be picked up before it can be completed");

        verify(activeDeliveryRepository, never()).save(any(ActiveDelivery.class));
    }

    @Test
    void updateLocationShouldRejectOutOfRangeCoordinates() {
        LocationUpdateRequestDto requestDto = new LocationUpdateRequestDto();
        requestDto.setCurrentLatitude(BigDecimal.valueOf(100));
        requestDto.setCurrentLongitude(BigDecimal.valueOf(77.5946));

        when(deliveryRepository.findByAgentId(7L)).thenReturn(Optional.of(agent));

        assertThatThrownBy(() -> deliveryService.updateLocation(7L, currentUser, requestDto))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Latitude must be between -90 and 90");
    }

    @Test
    void getNearbyAgentsShouldIgnoreAgentsWithoutLocationAndReturnOnlyInsideRadius() {
        DeliveryAgent nearbyAgent = new DeliveryAgent();
        nearbyAgent.setAgentId(7L);
        nearbyAgent.setUserId(10L);
        nearbyAgent.setFullName("Nearby Rider");
        nearbyAgent.setPhone("9999999999");
        nearbyAgent.setVehicleType(VehicleType.BIKE);
        nearbyAgent.setVehicleNumber("KA01AB1234");
        nearbyAgent.setCurrentLatitude(BigDecimal.valueOf(12.9716));
        nearbyAgent.setCurrentLongitude(BigDecimal.valueOf(77.5946));
        nearbyAgent.setAvailable(true);
        nearbyAgent.setVerified(true);

        DeliveryAgent missingLocationAgent = new DeliveryAgent();
        missingLocationAgent.setAgentId(8L);
        missingLocationAgent.setUserId(11L);
        missingLocationAgent.setFullName("Unknown Rider");
        missingLocationAgent.setPhone("8888888888");
        missingLocationAgent.setVehicleType(VehicleType.SCOOTER);
        missingLocationAgent.setVehicleNumber("KA02AB1234");
        missingLocationAgent.setAvailable(true);
        missingLocationAgent.setVerified(true);

        when(deliveryRepository.findByAvailableTrueAndVerifiedTrue())
                .thenReturn(List.of(nearbyAgent, missingLocationAgent));

        var response = deliveryService.getNearbyAgents(
                BigDecimal.valueOf(12.9716),
                BigDecimal.valueOf(77.5946),
                BigDecimal.valueOf(3)
        );

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getAgentId()).isEqualTo(7L);
    }

    @Test
    void getNearbyAgentsShouldRejectNonPositiveRadius() {
        assertThatThrownBy(() -> deliveryService.getNearbyAgents(
                BigDecimal.valueOf(12.9716),
                BigDecimal.valueOf(77.5946),
                BigDecimal.ZERO
        ))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Radius must be greater than 0");
    }
}
