package com.quickbite.restaurant.restaurantservice.service.serviceImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.quickbite.restaurant.restaurantservice.dto.requestDto.RestaurantRequestDto;
import com.quickbite.restaurant.restaurantservice.dto.responseDto.OwnerRestaurantDetailsResponseDto;
import com.quickbite.restaurant.restaurantservice.dto.responseDto.RestaurantOrderResponseDto;
import com.quickbite.restaurant.restaurantservice.dto.responseDto.RestaurantOwnerResponseDto;
import com.quickbite.restaurant.restaurantservice.dto.responseDto.RestaurantResponseDto;
import com.quickbite.restaurant.restaurantservice.entity.Restaurant;
import com.quickbite.restaurant.restaurantservice.event.NotificationEvent;
import com.quickbite.restaurant.restaurantservice.external.order.client.OrderClient;
import com.quickbite.restaurant.restaurantservice.repository.RestaurantRepository;
import com.quickbite.restaurant.restaurantservice.security.UserPrincipal;
import com.quickbite.restaurant.restaurantservice.service.NotificationEventPublisher;

@ExtendWith(MockitoExtension.class)
class RestaurantServiceImplTest {

    @Mock
    private RestaurantRepository repository;

    @Mock
    private NotificationEventPublisher notificationEventPublisher;

    @Mock
    private OrderClient orderClient;

    @InjectMocks
    private RestaurantServiceImpl restaurantService;

    @BeforeEach
    void setUp() {
        setAuthenticatedUser(7L, "owner@quickbite.com", "OWNER");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void registerRestaurantShouldPersistOwnerDefaultsAndPublishNotification() {
        RestaurantRequestDto request = new RestaurantRequestDto(
                "Spice Route",
                "Classic North Indian",
                "Indian",
                "MG Road",
                "Bengaluru",
                12.9716,
                77.5946,
                "9999999999",
                7.5,
                199.0,
                35
        );

        when(repository.save(any(Restaurant.class))).thenAnswer(invocation -> {
            Restaurant restaurant = invocation.getArgument(0);
            restaurant.setRestaurantId(101L);
            return restaurant;
        });

        RestaurantResponseDto response = restaurantService.registerRestaurant(request);

        assertThat(response.getRestaurantId()).isEqualTo(101L);
        assertThat(response.getName()).isEqualTo("Spice Route");

        ArgumentCaptor<Restaurant> restaurantCaptor = ArgumentCaptor.forClass(Restaurant.class);
        verify(repository).save(restaurantCaptor.capture());
        Restaurant savedRestaurant = restaurantCaptor.getValue();
        assertThat(savedRestaurant.getOwnerId()).isEqualTo(7L);
        assertThat(savedRestaurant.isApproved()).isFalse();
        assertThat(savedRestaurant.isOpen()).isFalse();

        ArgumentCaptor<NotificationEvent> notificationCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(notificationEventPublisher).publishRestaurantNotification(notificationCaptor.capture());
        assertThat(notificationCaptor.getValue().getEventType()).isEqualTo("RESTAURANT_SUBMITTED_FOR_APPROVAL");
    }

    @Test
    void registerRestaurantShouldRejectMissingLocation() {
        RestaurantRequestDto request = new RestaurantRequestDto(
                "Spice Route",
                "Classic North Indian",
                "Indian",
                "MG Road",
                "Bengaluru",
                null,
                null,
                "9999999999",
                7.5,
                199.0,
                35
        );

        assertThatThrownBy(() -> restaurantService.registerRestaurant(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Restaurant location is required");

        verify(repository, never()).save(any(Restaurant.class));
    }

    @Test
    void toggleOpenShouldRejectUnapprovedRestaurant() {
        Restaurant restaurant = new Restaurant();
        restaurant.setRestaurantId(10L);
        restaurant.setOwnerId(7L);
        restaurant.setApproved(false);

        when(repository.findByRestaurantIdAndOwnerId(10L, 7L)).thenReturn(Optional.of(restaurant));

        assertThatThrownBy(() -> restaurantService.toggleOpen(10L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Restaurant is not approved yet");

        verify(repository, never()).save(any(Restaurant.class));
    }

    @Test
    void approveRestaurantShouldMarkRestaurantApprovedAndNotifyOwner() {
        Restaurant restaurant = new Restaurant();
        restaurant.setRestaurantId(20L);
        restaurant.setOwnerId(88L);
        restaurant.setName("Cafe 88");
        restaurant.setApproved(false);
        restaurant.setRejectionReason("Old reason");

        when(repository.findById(20L)).thenReturn(Optional.of(restaurant));
        when(repository.save(restaurant)).thenReturn(restaurant);

        RestaurantResponseDto response = restaurantService.approveRestaurant(20L);

        assertThat(response.isApproved()).isTrue();
        assertThat(restaurant.getRejectionReason()).isNull();

        ArgumentCaptor<NotificationEvent> notificationCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(notificationEventPublisher).publishRestaurantNotification(notificationCaptor.capture());
        assertThat(notificationCaptor.getValue().getRecipientId()).isEqualTo(88L);
        assertThat(notificationCaptor.getValue().getEventType()).isEqualTo("RESTAURANT_APPROVED");
    }

    @Test
    void updateRatingShouldRecalculateAverageAndCount() {
        Restaurant restaurant = new Restaurant();
        restaurant.setRestaurantId(30L);
        restaurant.setAvgRating(4.0);
        restaurant.setRatingCount(2L);

        when(repository.findById(30L)).thenReturn(Optional.of(restaurant));
        when(repository.save(restaurant)).thenReturn(restaurant);

        RestaurantResponseDto response = restaurantService.updateRating(30L, 5.0);

        assertThat(response.getAvgRating()).isEqualTo(4.333333333333333);
        assertThat(restaurant.getRatingCount()).isEqualTo(3L);
        assertThat(restaurant.getAvgRating()).isEqualTo(4.333333333333333);
    }

    @Test
    void updateRatingShouldRejectOutOfRangeValues() {
        assertThatThrownBy(() -> restaurantService.updateRating(30L, 6.0))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Rating must be between 0 and 5");
    }

    @Test
    void getNearbyShouldReturnOnlyRestaurantsInsideRadius() {
        Restaurant nearbyRestaurant = new Restaurant();
        nearbyRestaurant.setRestaurantId(40L);
        nearbyRestaurant.setName("Central Kitchen");
        nearbyRestaurant.setLatitude(12.9716);
        nearbyRestaurant.setLongitude(77.5946);
        nearbyRestaurant.setApproved(true);

        Restaurant farRestaurant = new Restaurant();
        farRestaurant.setRestaurantId(41L);
        farRestaurant.setName("Far Away Diner");
        farRestaurant.setLatitude(28.6139);
        farRestaurant.setLongitude(77.2090);
        farRestaurant.setApproved(true);

        when(repository.findByIsApprovedTrue()).thenReturn(List.of(nearbyRestaurant, farRestaurant));

        List<RestaurantResponseDto> response = restaurantService.getNearby(12.9716, 77.5946, 10.0);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getRestaurantId()).isEqualTo(40L);
    }

    @Test
    void updateRestaurantShouldApplyOnlyProvidedFields() {
        Restaurant restaurant = new Restaurant();
        restaurant.setRestaurantId(50L);
        restaurant.setOwnerId(7L);
        restaurant.setName("Old Name");
        restaurant.setCity("Mumbai");
        restaurant.setPhone("1111111111");

        RestaurantRequestDto updateRequest = new RestaurantRequestDto();
        updateRequest.setName("New Name");
        updateRequest.setPhone("2222222222");

        when(repository.findByRestaurantIdAndOwnerId(50L, 7L)).thenReturn(Optional.of(restaurant));
        when(repository.save(restaurant)).thenReturn(restaurant);

        RestaurantResponseDto response = restaurantService.updateRestaurant(50L, updateRequest);

        assertThat(response.getName()).isEqualTo("New Name");
        assertThat(restaurant.getCity()).isEqualTo("Mumbai");
        assertThat(restaurant.getPhone()).isEqualTo("2222222222");
    }

    @Test
    void updateRestaurantShouldRejectPartialLocationUpdates() {
        Restaurant restaurant = new Restaurant();
        restaurant.setRestaurantId(50L);
        restaurant.setOwnerId(7L);
        restaurant.setName("Old Name");

        RestaurantRequestDto updateRequest = new RestaurantRequestDto();
        updateRequest.setLatitude(12.9716);

        when(repository.findByRestaurantIdAndOwnerId(50L, 7L)).thenReturn(Optional.of(restaurant));

        assertThatThrownBy(() -> restaurantService.updateRestaurant(50L, updateRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Latitude and longitude must be provided together");

        verify(repository, never()).save(any(Restaurant.class));
    }

    @Test
    void getByOwnerShouldFailWhenAuthenticationMissing() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> restaurantService.getByOwner())
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Unauthorized access");
    }

    @Test
    void getOwnerRestaurantDetailsShouldReturnRestaurantWithOrders() {
        Restaurant restaurant = new Restaurant();
        restaurant.setRestaurantId(60L);
        restaurant.setOwnerId(7L);
        restaurant.setName("Order House");
        restaurant.setApproved(true);

        RestaurantOrderResponseDto order = new RestaurantOrderResponseDto();
        order.setOrderId(501L);
        order.setOrderStatus("CONFIRMED");

        when(repository.findByRestaurantIdAndOwnerId(60L, 7L)).thenReturn(Optional.of(restaurant));
        when(orderClient.getOrdersByRestaurant(60L, "Bearer owner-token")).thenReturn(List.of(order));

        OwnerRestaurantDetailsResponseDto response =
                restaurantService.getOwnerRestaurantDetails(60L, "Bearer owner-token");

        assertThat(response.getRestaurant().getRestaurantId()).isEqualTo(60L);
        assertThat(response.getOrders()).hasSize(1);
        assertThat(response.getOrders().get(0).getOrderId()).isEqualTo(501L);
    }

    @Test
    void getRestaurantOwnerInfoShouldReturnOwnerPayload() {
        Restaurant restaurant = new Restaurant();
        restaurant.setRestaurantId(70L);
        restaurant.setOwnerId(17L);
        restaurant.setName("Owner Lookup");

        when(repository.findById(70L)).thenReturn(Optional.of(restaurant));

        RestaurantOwnerResponseDto response = restaurantService.getRestaurantOwnerInfo(70L);

        assertThat(response.getRestaurantId()).isEqualTo(70L);
        assertThat(response.getOwnerId()).isEqualTo(17L);
        assertThat(response.getName()).isEqualTo("Owner Lookup");
    }

    private void setAuthenticatedUser(Long userId, String email, String role) {
        UserPrincipal userPrincipal = new UserPrincipal(userId, email, role);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userPrincipal, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
