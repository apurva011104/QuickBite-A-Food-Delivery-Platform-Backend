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

import com.quickbite.delivery.deliveryservice.client.OrderClient;
import com.quickbite.delivery.deliveryservice.client.dto.OrderSummaryResponseDto;
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
import com.quickbite.delivery.deliveryservice.service.DeliveryOtpMailService;
import com.quickbite.delivery.deliveryservice.service.NotificationEventPublisher;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceImplTest {

    @Mock
    private DeliveryRepository deliveryRepository;

    @Mock
    private ActiveDeliveryRepository activeDeliveryRepository;

    @Mock
    private NotificationEventPublisher notificationEventPublisher;

    @Mock
    private OrderClient orderClient;

    @Mock
    private DeliveryOtpMailService deliveryOtpMailService;

    private DeliveryServiceImpl deliveryService;
    private DeliveryAgent agent;
    private UserPrincipal currentUser;
    private UserPrincipal customerUser;

    @BeforeEach
    void setUp() {
        deliveryService = new DeliveryServiceImpl();
        ReflectionTestUtils.setField(deliveryService, "deliveryRepository", deliveryRepository);
        ReflectionTestUtils.setField(deliveryService, "activeDeliveryRepository", activeDeliveryRepository);
        ReflectionTestUtils.setField(deliveryService, "deliveryAgentMapper", new DeliveryAgentMapper());
        ReflectionTestUtils.setField(deliveryService, "notificationEventPublisher", notificationEventPublisher);
        ReflectionTestUtils.setField(deliveryService, "orderClient", orderClient);
        ReflectionTestUtils.setField(deliveryService, "deliveryOtpMailService", deliveryOtpMailService);

        currentUser = new UserPrincipal(10L, "agent@quickbite.com", "AGENT");
        customerUser = new UserPrincipal(21L, "customer@quickbite.com", "CUSTOMER");

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
        OrderSummaryResponseDto order = new OrderSummaryResponseDto();
        order.setOrderId(101L);
        order.setOrderStatus("READY_FOR_PICKUP");

        when(deliveryRepository.findByAgentId(7L)).thenReturn(Optional.of(agent));
        when(orderClient.getOrderById(101L, "Bearer admin-token")).thenReturn(order);
        when(activeDeliveryRepository.existsByOrderIdAndStatusIn(any(Long.class), any(List.class))).thenReturn(false);
        when(activeDeliveryRepository.save(any(ActiveDelivery.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(deliveryRepository.save(any(DeliveryAgent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = deliveryService.assignOrder(requestDto, "Bearer admin-token");

        assertThat(response.getMessage()).contains("Order 101 assigned");
        assertThat(agent.isAvailable()).isFalse();

        ArgumentCaptor<ActiveDelivery> captor = ArgumentCaptor.forClass(ActiveDelivery.class);
        verify(activeDeliveryRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(DeliveryStatus.ASSIGNED);
        verify(orderClient).assignDeliveryAgent(101L, 10L, "Bearer admin-token");
        verify(notificationEventPublisher).publishDeliveryNotification(any(NotificationEvent.class));
    }

    @Test
    void assignOrderShouldRejectWhenOrderIsNotReadyForPickup() {
        AssignOrderRequestDto requestDto = new AssignOrderRequestDto();
        requestDto.setAgentId(7L);
        requestDto.setOrderId(101L);
        OrderSummaryResponseDto order = new OrderSummaryResponseDto();
        order.setOrderId(101L);
        order.setOrderStatus("PREPARING");

        when(deliveryRepository.findByAgentId(7L)).thenReturn(Optional.of(agent));
        when(orderClient.getOrderById(101L, "Bearer admin-token")).thenReturn(order);

        assertThatThrownBy(() -> deliveryService.assignOrder(requestDto, "Bearer admin-token"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Only READY_FOR_PICKUP orders can be assigned to an agent");

        verify(activeDeliveryRepository, never()).save(any(ActiveDelivery.class));
    }

    @Test
    void acceptDeliveryShouldMoveAssignedDeliveryToAccepted() {
        PickupDeliveryRequestDto requestDto = new PickupDeliveryRequestDto();
        requestDto.setAgentId(7L);
        requestDto.setOrderId(101L);

        ActiveDelivery activeDelivery = new ActiveDelivery(101L, 7L, DeliveryStatus.ASSIGNED);

        when(deliveryRepository.findByAgentId(7L)).thenReturn(Optional.of(agent));
        when(activeDeliveryRepository.findTopByOrderIdOrderByCreatedAtDesc(101L)).thenReturn(Optional.of(activeDelivery));
        when(activeDeliveryRepository.save(any(ActiveDelivery.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = deliveryService.acceptDelivery(currentUser, requestDto);

        assertThat(response.getMessage()).contains("accepted");
        assertThat(activeDelivery.getStatus()).isEqualTo(DeliveryStatus.ACCEPTED);
    }

    @Test
    void rejectDeliveryShouldMarkAssignmentRejectedAndSetAgentOnline() {
        PickupDeliveryRequestDto requestDto = new PickupDeliveryRequestDto();
        requestDto.setAgentId(7L);
        requestDto.setOrderId(101L);

        ActiveDelivery activeDelivery = new ActiveDelivery(101L, 7L, DeliveryStatus.ASSIGNED);
        agent.setAvailable(false);

        when(deliveryRepository.findByAgentId(7L)).thenReturn(Optional.of(agent));
        when(activeDeliveryRepository.findTopByOrderIdOrderByCreatedAtDesc(101L)).thenReturn(Optional.of(activeDelivery));
        when(activeDeliveryRepository.save(any(ActiveDelivery.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(deliveryRepository.save(any(DeliveryAgent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = deliveryService.rejectDelivery(currentUser, requestDto);

        assertThat(response.getMessage()).contains("rejected");
        assertThat(activeDelivery.getStatus()).isEqualTo(DeliveryStatus.REJECTED);
        assertThat(agent.isAvailable()).isTrue();
    }

    @Test
    void pickupDeliveryShouldMoveAcceptedDeliveryToPickedUp() {
        PickupDeliveryRequestDto requestDto = new PickupDeliveryRequestDto();
        requestDto.setAgentId(7L);
        requestDto.setOrderId(101L);
        OrderSummaryResponseDto order = buildOrderSummary("READY_FOR_PICKUP");

        ActiveDelivery activeDelivery = new ActiveDelivery(101L, 7L, DeliveryStatus.ACCEPTED);

        when(deliveryRepository.findByAgentId(7L)).thenReturn(Optional.of(agent));
        when(orderClient.getOrderById(101L, "Bearer agent-token")).thenReturn(order);
        when(activeDeliveryRepository.findTopByOrderIdOrderByCreatedAtDesc(101L)).thenReturn(Optional.of(activeDelivery));
        when(activeDeliveryRepository.save(any(ActiveDelivery.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = deliveryService.pickupDelivery(currentUser, requestDto, "Bearer agent-token");

        assertThat(response.getMessage()).contains("picked up");
        assertThat(activeDelivery.getStatus()).isEqualTo(DeliveryStatus.PICKED_UP);
        assertThat(activeDelivery.getCompletionOtp()).matches("\\d{6}");
        verify(orderClient).updateOrderStatus(101L, "OUT_FOR_DELIVERY", "Bearer agent-token");
        verify(notificationEventPublisher).publishDeliveryNotification(any(NotificationEvent.class));
    }

    @Test
    void pickupDeliveryShouldRejectWhenAssignmentIsNotAcceptedYet() {
        PickupDeliveryRequestDto requestDto = new PickupDeliveryRequestDto();
        requestDto.setAgentId(7L);
        requestDto.setOrderId(101L);

        ActiveDelivery activeDelivery = new ActiveDelivery(101L, 7L, DeliveryStatus.ASSIGNED);

        when(deliveryRepository.findByAgentId(7L)).thenReturn(Optional.of(agent));
        when(activeDeliveryRepository.findTopByOrderIdOrderByCreatedAtDesc(101L)).thenReturn(Optional.of(activeDelivery));

        assertThatThrownBy(() -> deliveryService.pickupDelivery(currentUser, requestDto, "Bearer agent-token"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Delivery can only be picked up after the agent accepts it");
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

        assertThatThrownBy(() -> deliveryService.pickupDelivery(currentUser, requestDto, "Bearer agent-token"))
                .isInstanceOf(UnauthorizedActionException.class)
                .hasMessage("You are not allowed to perform this action");
    }

    @Test
    void completeDeliveryShouldRejectWhenOrderNotPickedUpYet() {
        CompleteDeliveryRequestDto requestDto = new CompleteDeliveryRequestDto();
        requestDto.setAgentId(7L);
        requestDto.setOrderId(101L);

        ActiveDelivery activeDelivery = new ActiveDelivery(101L, 7L, DeliveryStatus.ACCEPTED);

        when(deliveryRepository.findByAgentId(7L)).thenReturn(Optional.of(agent));
        when(activeDeliveryRepository.findTopByOrderIdOrderByCreatedAtDesc(101L)).thenReturn(Optional.of(activeDelivery));

        assertThatThrownBy(() -> deliveryService.completeDelivery(currentUser, requestDto, "Bearer agent-token"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Delivery must be picked up before it can be completed");

        verify(activeDeliveryRepository, never()).save(any(ActiveDelivery.class));
    }

    @Test
    void completeDeliveryShouldSyncOrderAndSetAgentOnline() {
        CompleteDeliveryRequestDto requestDto = new CompleteDeliveryRequestDto();
        requestDto.setAgentId(7L);
        requestDto.setOrderId(101L);
        requestDto.setOtp("654321");

        ActiveDelivery activeDelivery = new ActiveDelivery(101L, 7L, DeliveryStatus.PICKED_UP);
        activeDelivery.setCompletionOtp("654321");
        agent.setAvailable(false);

        when(deliveryRepository.findByAgentId(7L)).thenReturn(Optional.of(agent));
        when(activeDeliveryRepository.findTopByOrderIdOrderByCreatedAtDesc(101L)).thenReturn(Optional.of(activeDelivery));
        when(activeDeliveryRepository.save(any(ActiveDelivery.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(deliveryRepository.save(any(DeliveryAgent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = deliveryService.completeDelivery(currentUser, requestDto, "Bearer agent-token");

        assertThat(response.getMessage()).contains("marked as delivered");
        assertThat(activeDelivery.getStatus()).isEqualTo(DeliveryStatus.DELIVERED);
        assertThat(agent.isAvailable()).isTrue();
        verify(orderClient).updateOrderStatus(101L, "DELIVERED", "Bearer agent-token");
    }

    @Test
    void completeDeliveryShouldRejectInvalidOtp() {
        CompleteDeliveryRequestDto requestDto = new CompleteDeliveryRequestDto();
        requestDto.setAgentId(7L);
        requestDto.setOrderId(101L);
        requestDto.setOtp("000000");

        ActiveDelivery activeDelivery = new ActiveDelivery(101L, 7L, DeliveryStatus.PICKED_UP);
        activeDelivery.setCompletionOtp("123456");

        when(deliveryRepository.findByAgentId(7L)).thenReturn(Optional.of(agent));
        when(activeDeliveryRepository.findTopByOrderIdOrderByCreatedAtDesc(101L)).thenReturn(Optional.of(activeDelivery));

        assertThatThrownBy(() -> deliveryService.completeDelivery(currentUser, requestDto, "Bearer agent-token"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Invalid delivery confirmation OTP");
    }

    @Test
    void getCompletionOtpShouldReturnOtpForOwningCustomer() {
        ActiveDelivery activeDelivery = new ActiveDelivery(101L, 7L, DeliveryStatus.PICKED_UP);
        activeDelivery.setCompletionOtp("123456");
        OrderSummaryResponseDto order = buildOrderSummary("OUT_FOR_DELIVERY");

        when(orderClient.getOrderById(101L, "Bearer customer-token")).thenReturn(order);
        when(activeDeliveryRepository.findTopByOrderIdOrderByCreatedAtDesc(101L)).thenReturn(Optional.of(activeDelivery));
        when(deliveryOtpMailService.sendDeliveryCompletionOtp("customer@quickbite.com", 101L, "123456"))
                .thenReturn(true);
        when(activeDeliveryRepository.save(any(ActiveDelivery.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = deliveryService.getCompletionOtp(101L, customerUser, "Bearer customer-token");

        assertThat(response.getOtp()).isEqualTo("123456");
        assertThat(response.getOrderId()).isEqualTo(101L);
        assertThat(activeDelivery.getCompletionOtpEmailSentAt()).isNotNull();
        verify(deliveryOtpMailService).sendDeliveryCompletionOtp("customer@quickbite.com", 101L, "123456");
    }

    @Test
    void getCompletionOtpShouldRejectDifferentCustomer() {
        UserPrincipal otherCustomer = new UserPrincipal(99L, "other@quickbite.com", "CUSTOMER");
        OrderSummaryResponseDto order = buildOrderSummary("OUT_FOR_DELIVERY");

        when(orderClient.getOrderById(101L, "Bearer customer-token")).thenReturn(order);

        assertThatThrownBy(() -> deliveryService.getCompletionOtp(101L, otherCustomer, "Bearer customer-token"))
                .isInstanceOf(UnauthorizedActionException.class)
                .hasMessage("You are not allowed to view this delivery confirmation OTP");
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

    private OrderSummaryResponseDto buildOrderSummary(String orderStatus) {
        OrderSummaryResponseDto order = new OrderSummaryResponseDto();
        order.setOrderId(101L);
        order.setOrderStatus(orderStatus);
        order.setCustomerId(customerUser.getUserId());
        return order;
    }
}
