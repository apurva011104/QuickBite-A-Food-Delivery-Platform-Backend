package com.quickbite.review.reviewservice.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.quickbite.review.reviewservice.dto.requestDto.MenuItemReviewRequestDto;
import com.quickbite.review.reviewservice.dto.requestDto.ReviewRequestDto;
import com.quickbite.review.reviewservice.dto.responseDto.ReviewResponseDto;
import com.quickbite.review.reviewservice.entity.MenuItemReview;
import com.quickbite.review.reviewservice.entity.Review;
import com.quickbite.review.reviewservice.exception.BadRequestException;
import com.quickbite.review.reviewservice.exception.UnauthorizedActionException;
import com.quickbite.review.reviewservice.external.delivery.client.DeliveryClient;
import com.quickbite.review.reviewservice.external.delivery.dto.DeliveryRatingUpdateRequestDto;
import com.quickbite.review.reviewservice.external.order.client.OrderClient;
import com.quickbite.review.reviewservice.external.order.dto.OrderItemSummaryResponseDto;
import com.quickbite.review.reviewservice.external.order.dto.OrderSummaryResponseDto;
import com.quickbite.review.reviewservice.external.restaurant.client.RestaurantClient;
import com.quickbite.review.reviewservice.mapper.ReviewMapper;
import com.quickbite.review.reviewservice.repository.MenuItemReviewRepository;
import com.quickbite.review.reviewservice.repository.ReviewRepository;
import com.quickbite.review.reviewservice.security.UserPrincipal;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private MenuItemReviewRepository menuItemReviewRepository;

    @Mock
    private RestaurantClient restaurantClient;

    @Mock
    private DeliveryClient deliveryClient;

    @Mock
    private OrderClient orderClient;

    private ReviewServiceImpl reviewService;
    private UserPrincipal customerUser;

    @BeforeEach
    void setUp() {
        reviewService = new ReviewServiceImpl(
                reviewRepository,
                menuItemReviewRepository,
                new ReviewMapper(),
                restaurantClient,
                deliveryClient,
                orderClient
        );
        customerUser = new UserPrincipal(7L, "customer@quickbite.com", "CUSTOMER");
    }

    @Test
    void addReviewShouldPersistDeliveryAndMenuItemReviews() {
        ReviewRequestDto requestDto = buildReviewRequest();
        OrderSummaryResponseDto order = buildDeliveredOrder();

        when(reviewRepository.existsByOrderId(100L)).thenReturn(false);
        when(orderClient.getOrderById(100L)).thenReturn(order);
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
            Review review = invocation.getArgument(0);
            review.setReviewId(1L);
            return review;
        });
        when(menuItemReviewRepository.saveAll(anyList())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            List<MenuItemReview> itemReviews = invocation.getArgument(0);
            long id = 1L;
            for (MenuItemReview itemReview : itemReviews) {
                itemReview.setMenuItemReviewId(id++);
            }
            return itemReviews;
        });

        ReviewResponseDto response = reviewService.addReview(customerUser, requestDto);

        assertThat(response.getReviewId()).isEqualTo(1L);
        assertThat(response.getRestaurantId()).isEqualTo(11L);
        assertThat(response.getAgentId()).isEqualTo(22L);
        assertThat(response.getFoodRating()).isEqualTo(5);
        assertThat(response.getItemReviews()).hasSize(2);
        assertThat(response.getItemReviews().get(0).getItemName()).isEqualTo("Paneer Wrap");

        verify(restaurantClient).updateRestaurantRating(11L, 5.0);
        verify(deliveryClient).updateDeliveryRating(
                eq(22L),
                argThat((DeliveryRatingUpdateRequestDto dto) -> dto.getNewRating().equals(4))
        );
    }

    @Test
    void addReviewShouldRejectWhenOrderIsNotDelivered() {
        ReviewRequestDto requestDto = buildReviewRequest();
        OrderSummaryResponseDto order = buildDeliveredOrder();
        order.setOrderStatus("OUT_FOR_DELIVERY");

        when(reviewRepository.existsByOrderId(100L)).thenReturn(false);
        when(orderClient.getOrderById(100L)).thenReturn(order);

        assertThatThrownBy(() -> reviewService.addReview(customerUser, requestDto))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Reviews can only be submitted after the order is delivered");
    }

    @Test
    void addReviewShouldRejectWhenMenuItemIsNotPartOfOrder() {
        ReviewRequestDto requestDto = buildReviewRequest();
        requestDto.getItemReviews().get(1).setMenuItemId(999L);

        when(reviewRepository.existsByOrderId(100L)).thenReturn(false);
        when(orderClient.getOrderById(100L)).thenReturn(buildDeliveredOrder());

        assertThatThrownBy(() -> reviewService.addReview(customerUser, requestDto))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Menu item 999 is not part of this order");
    }

    @Test
    void getByOrderIdShouldRejectDifferentCustomer() {
        Review review = buildSavedReview();
        UserPrincipal otherCustomer = new UserPrincipal(99L, "other@quickbite.com", "CUSTOMER");

        when(reviewRepository.findByOrderId(100L)).thenReturn(java.util.Optional.of(review));

        assertThatThrownBy(() -> reviewService.getByOrderId(100L, otherCustomer))
                .isInstanceOf(UnauthorizedActionException.class)
                .hasMessage("You are not allowed to view this review");
    }

    @Test
    void getByOrderIdShouldIncludeMenuItemReviews() {
        Review review = buildSavedReview();
        MenuItemReview firstItemReview = new MenuItemReview();
        firstItemReview.setMenuItemReviewId(1L);
        firstItemReview.setOrderId(100L);
        firstItemReview.setMenuItemId(501L);
        firstItemReview.setItemName("Paneer Wrap");
        firstItemReview.setRating(5);
        firstItemReview.setComment("Excellent");
        firstItemReview.setVerified(false);

        when(reviewRepository.findByOrderId(100L)).thenReturn(java.util.Optional.of(review));
        when(menuItemReviewRepository.findByOrderId(100L)).thenReturn(List.of(firstItemReview));

        ReviewResponseDto response = reviewService.getByOrderId(100L, customerUser);

        assertThat(response.getItemReviews()).hasSize(1);
        assertThat(response.getItemReviews().get(0).getMenuItemId()).isEqualTo(501L);
        assertThat(response.getItemReviews().get(0).getItemName()).isEqualTo("Paneer Wrap");
    }

    @Test
    void getMenuItemReviewSummaryShouldReturnAverageAndCount() {
        MenuItemReview firstItemReview = new MenuItemReview();
        firstItemReview.setMenuItemReviewId(1L);
        firstItemReview.setMenuItemId(501L);
        firstItemReview.setRating(5);

        MenuItemReview secondItemReview = new MenuItemReview();
        secondItemReview.setMenuItemReviewId(2L);
        secondItemReview.setMenuItemId(501L);
        secondItemReview.setRating(3);

        when(menuItemReviewRepository.findByMenuItemId(501L)).thenReturn(List.of(firstItemReview, secondItemReview));
        when(menuItemReviewRepository.countByMenuItemId(501L)).thenReturn(2L);

        var response = reviewService.getMenuItemReviewSummary(501L);

        assertThat(response.getMenuItemId()).isEqualTo(501L);
        assertThat(response.getAverageRating()).isEqualTo(4.0);
        assertThat(response.getReviewCount()).isEqualTo(2L);
    }

    private ReviewRequestDto buildReviewRequest() {
        MenuItemReviewRequestDto firstItem = new MenuItemReviewRequestDto();
        firstItem.setMenuItemId(501L);
        firstItem.setRating(5);
        firstItem.setComment("Fresh and tasty");

        MenuItemReviewRequestDto secondItem = new MenuItemReviewRequestDto();
        secondItem.setMenuItemId(502L);
        secondItem.setRating(4);
        secondItem.setComment("Good crunch");

        ReviewRequestDto requestDto = new ReviewRequestDto();
        requestDto.setOrderId(100L);
        requestDto.setDeliveryRating(4);
        requestDto.setComment("Delivery was smooth and on time.");
        requestDto.setItemReviews(List.of(firstItem, secondItem));
        return requestDto;
    }

    private OrderSummaryResponseDto buildDeliveredOrder() {
        OrderItemSummaryResponseDto firstItem = new OrderItemSummaryResponseDto();
        firstItem.setOrderItemId(1L);
        firstItem.setMenuItemId(501L);
        firstItem.setName("Paneer Wrap");

        OrderItemSummaryResponseDto secondItem = new OrderItemSummaryResponseDto();
        secondItem.setOrderItemId(2L);
        secondItem.setMenuItemId(502L);
        secondItem.setName("Fries");

        OrderSummaryResponseDto order = new OrderSummaryResponseDto();
        order.setOrderId(100L);
        order.setCustomerId(7L);
        order.setRestaurantId(11L);
        order.setDeliveryAgentId(22L);
        order.setOrderStatus("DELIVERED");
        order.setItems(List.of(firstItem, secondItem));
        return order;
    }

    private Review buildSavedReview() {
        Review review = new Review();
        review.setReviewId(1L);
        review.setOrderId(100L);
        review.setCustomerId(7L);
        review.setRestaurantId(11L);
        review.setAgentId(22L);
        review.setFoodRating(5);
        review.setDeliveryRating(4);
        review.setComment("Delivery was smooth and on time.");
        review.setVerified(false);
        return review;
    }
}
