package com.quickbite.review.reviewservice.service.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.quickbite.review.reviewservice.dto.requestDto.MenuItemReviewRequestDto;
import com.quickbite.review.reviewservice.dto.requestDto.ReviewModerationRequestDto;
import com.quickbite.review.reviewservice.dto.requestDto.ReviewRequestDto;
import com.quickbite.review.reviewservice.dto.requestDto.ReviewUpdateRequestDto;
import com.quickbite.review.reviewservice.dto.responseDto.MenuItemReviewResponseDto;
import com.quickbite.review.reviewservice.dto.responseDto.MenuItemReviewSummaryResponseDto;
import com.quickbite.review.reviewservice.dto.responseDto.MessageResponseDto;
import com.quickbite.review.reviewservice.dto.responseDto.ReviewResponseDto;
import com.quickbite.review.reviewservice.entity.MenuItemReview;
import com.quickbite.review.reviewservice.entity.Review;
import com.quickbite.review.reviewservice.exception.BadRequestException;
import com.quickbite.review.reviewservice.exception.ResourceNotFoundException;
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
import com.quickbite.review.reviewservice.service.ReviewService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final MenuItemReviewRepository menuItemReviewRepository;
    private final ReviewMapper reviewMapper;
    private final RestaurantClient restaurantClient;
    private final DeliveryClient deliveryClient;
    private final OrderClient orderClient;

    @Override
    public ReviewResponseDto addReview(UserPrincipal currentUser, ReviewRequestDto requestDto) {
        log.info("Adding review for orderId={} customerId={}", requestDto.getOrderId(), currentUser.getUserId());

        if (reviewRepository.existsByOrderId(requestDto.getOrderId())) {
            throw new BadRequestException("Review already exists for this order");
        }

        OrderSummaryResponseDto order = getOrderOrThrow(requestDto.getOrderId());
        validateReviewableOrder(order, currentUser);

        if (order.getDeliveryAgentId() == null) {
            throw new BadRequestException("This delivered order does not have an assigned delivery agent to review");
        }

        Map<Long, OrderItemSummaryResponseDto> orderItemsByMenuItemId = buildOrderItemLookup(order);
        validateItemReviews(requestDto.getItemReviews(), orderItemsByMenuItemId);

        Review review = reviewMapper.toEntity(requestDto);
        review.setCustomerId(currentUser.getUserId());
        review.setRestaurantId(order.getRestaurantId());
        review.setAgentId(order.getDeliveryAgentId());
        review.setFoodRating(calculateFoodRating(requestDto.getItemReviews()));
        review.setVerified(false);

        Review savedReview = reviewRepository.save(review);
        List<MenuItemReview> savedMenuItemReviews = menuItemReviewRepository.saveAll(
                buildMenuItemReviews(savedReview, requestDto.getItemReviews(), orderItemsByMenuItemId)
        );

        log.info("Review created successfully reviewId={} with {} menu item reviews",
                savedReview.getReviewId(), savedMenuItemReviews.size());

        pushRatings(savedReview);

        return buildDetailedResponse(savedReview, savedMenuItemReviews);
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewResponseDto getByReviewId(Long reviewId) {
        Review review = reviewRepository.findByReviewId(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with ID: " + reviewId));

        return buildDetailedResponse(review, menuItemReviewRepository.findByOrderId(review.getOrderId()));
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewResponseDto getByOrderId(Long orderId, UserPrincipal currentUser) {
        Review review = reviewRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found for order ID: " + orderId));

        authorizeReviewAccess(review, currentUser);
        return buildDetailedResponse(review, menuItemReviewRepository.findByOrderId(orderId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponseDto> getByRestaurantId(Long restaurantId) {
        return reviewRepository.findByRestaurantId(restaurantId)
                .stream()
                .map(review -> buildDetailedResponse(review, menuItemReviewRepository.findByOrderId(review.getOrderId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponseDto> getByCustomerId(Long customerId, UserPrincipal currentUser) {
        if (!customerId.equals(currentUser.getUserId()) && !"ADMIN".equals(currentUser.getRole())) {
            throw new UnauthorizedActionException("You are not allowed to view these reviews");
        }

        return reviewRepository.findByCustomerId(customerId)
                .stream()
                .map(review -> buildDetailedResponse(review, menuItemReviewRepository.findByOrderId(review.getOrderId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponseDto> getByAgentId(Long agentId) {
        return reviewRepository.findByAgentId(agentId)
                .stream()
                .map(review -> buildDetailedResponse(review, menuItemReviewRepository.findByOrderId(review.getOrderId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MenuItemReviewResponseDto> getByMenuItemId(Long menuItemId) {
        return reviewMapper.toMenuItemResponseDtos(menuItemReviewRepository.findByMenuItemId(menuItemId));
    }

    @Override
    @Transactional(readOnly = true)
    public MenuItemReviewSummaryResponseDto getMenuItemReviewSummary(Long menuItemId) {
        return new MenuItemReviewSummaryResponseDto(
                menuItemId,
                getAvgMenuItemRating(menuItemId),
                menuItemReviewRepository.countByMenuItemId(menuItemId)
        );
    }

    @Override
    public ReviewResponseDto updateReview(Long reviewId, UserPrincipal currentUser, ReviewUpdateRequestDto requestDto) {
        Review review = reviewRepository.findByReviewId(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with ID: " + reviewId));

        if (!review.getCustomerId().equals(currentUser.getUserId())) {
            throw new UnauthorizedActionException("You are not allowed to update this review");
        }

        if (requestDto.getFoodRating() != null) {
            review.setFoodRating(requestDto.getFoodRating());
        }

        if (requestDto.getDeliveryRating() != null) {
            review.setDeliveryRating(requestDto.getDeliveryRating());
        }

        if (requestDto.getComment() != null) {
            review.setComment(requestDto.getComment());
        }

        review.setVerified(false);

        Review updatedReview = reviewRepository.save(review);
        log.info("Review updated successfully reviewId={}", reviewId);

        pushRatings(updatedReview);

        return buildDetailedResponse(updatedReview, menuItemReviewRepository.findByOrderId(updatedReview.getOrderId()));
    }

    @Override
    public ReviewResponseDto moderateReview(Long reviewId, ReviewModerationRequestDto requestDto) {
        Review review = reviewRepository.findByReviewId(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with ID: " + reviewId));

        review.setVerified(requestDto.getVerified());
        Review updatedReview = reviewRepository.save(review);

        log.info("Review moderation updated reviewId={} verified={}", reviewId, requestDto.getVerified());
        return buildDetailedResponse(updatedReview, menuItemReviewRepository.findByOrderId(updatedReview.getOrderId()));
    }

    @Override
    public MessageResponseDto deleteReview(Long reviewId, UserPrincipal currentUser) {
        Review review = reviewRepository.findByReviewId(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with ID: " + reviewId));

        if (!review.getCustomerId().equals(currentUser.getUserId()) && !"ADMIN".equals(currentUser.getRole())) {
            throw new UnauthorizedActionException("You are not allowed to delete this review");
        }

        menuItemReviewRepository.deleteAll(menuItemReviewRepository.findByOrderId(review.getOrderId()));
        reviewRepository.delete(review);
        log.info("Review deleted successfully reviewId={}", reviewId);

        return new MessageResponseDto("Review deleted successfully");
    }

    @Override
    @Transactional(readOnly = true)
    public Double getAvgFoodRating(Long restaurantId) {
        List<Review> reviews = reviewRepository.findByRestaurantId(restaurantId);
        if (reviews.isEmpty()) {
            return 0.0;
        }

        return reviews.stream()
                .mapToInt(Review::getFoodRating)
                .average()
                .orElse(0.0);
    }

    @Override
    @Transactional(readOnly = true)
    public Double getAvgDeliveryRating(Long agentId) {
        List<Review> reviews = reviewRepository.findByAgentId(agentId);
        if (reviews.isEmpty()) {
            return 0.0;
        }

        return reviews.stream()
                .mapToInt(Review::getDeliveryRating)
                .average()
                .orElse(0.0);
    }

    @Override
    @Transactional(readOnly = true)
    public Double getAvgMenuItemRating(Long menuItemId) {
        List<MenuItemReview> itemReviews = menuItemReviewRepository.findByMenuItemId(menuItemId);
        if (itemReviews.isEmpty()) {
            return 0.0;
        }

        return itemReviews.stream()
                .mapToInt(MenuItemReview::getRating)
                .average()
                .orElse(0.0);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponseDto> getAllReviews() {
        return reviewRepository.findAll()
                .stream()
                .map(review -> buildDetailedResponse(review, menuItemReviewRepository.findByOrderId(review.getOrderId())))
                .toList();
    }

    private OrderSummaryResponseDto getOrderOrThrow(Long orderId) {
        try {
            return orderClient.getOrderById(orderId);
        } catch (Exception ex) {
            log.error("Unable to fetch order {} for review validation", orderId, ex);
            throw new BadRequestException("Unable to validate order details for review submission");
        }
    }

    private void validateReviewableOrder(OrderSummaryResponseDto order, UserPrincipal currentUser) {
        if (!order.getCustomerId().equals(currentUser.getUserId())) {
            throw new UnauthorizedActionException("You are not allowed to review this order");
        }

        if (!"DELIVERED".equals(order.getOrderStatus())) {
            throw new BadRequestException("Reviews can only be submitted after the order is delivered");
        }
    }

    private Map<Long, OrderItemSummaryResponseDto> buildOrderItemLookup(OrderSummaryResponseDto order) {
        Map<Long, OrderItemSummaryResponseDto> itemsByMenuItemId = new HashMap<>();
        if (order.getItems() == null || order.getItems().isEmpty()) {
            throw new BadRequestException("This order does not contain any items to review");
        }

        for (OrderItemSummaryResponseDto item : order.getItems()) {
            if (item.getMenuItemId() != null) {
                itemsByMenuItemId.putIfAbsent(item.getMenuItemId(), item);
            }
        }
        return itemsByMenuItemId;
    }

    private void validateItemReviews(List<MenuItemReviewRequestDto> itemReviews,
                                     Map<Long, OrderItemSummaryResponseDto> orderItemsByMenuItemId) {
        Set<Long> reviewedMenuItemIds = new HashSet<>();

        for (MenuItemReviewRequestDto itemReview : itemReviews) {
            if (!orderItemsByMenuItemId.containsKey(itemReview.getMenuItemId())) {
                throw new BadRequestException("Menu item " + itemReview.getMenuItemId() + " is not part of this order");
            }

            if (!reviewedMenuItemIds.add(itemReview.getMenuItemId())) {
                throw new BadRequestException("Duplicate menu item review found for item " + itemReview.getMenuItemId());
            }
        }

        if (reviewedMenuItemIds.size() != orderItemsByMenuItemId.size()) {
            throw new BadRequestException("Please review each menu item in the delivered order");
        }
    }

    private Integer calculateFoodRating(List<MenuItemReviewRequestDto> itemReviews) {
        double average = itemReviews.stream()
                .mapToInt(MenuItemReviewRequestDto::getRating)
                .average()
                .orElse(0.0);

        return (int) Math.round(average);
    }

    private List<MenuItemReview> buildMenuItemReviews(Review review,
                                                      List<MenuItemReviewRequestDto> itemReviews,
                                                      Map<Long, OrderItemSummaryResponseDto> orderItemsByMenuItemId) {
        return itemReviews.stream().map(itemReview -> {
            OrderItemSummaryResponseDto orderItem = orderItemsByMenuItemId.get(itemReview.getMenuItemId());

            MenuItemReview menuItemReview = new MenuItemReview();
            menuItemReview.setOrderId(review.getOrderId());
            menuItemReview.setCustomerId(review.getCustomerId());
            menuItemReview.setRestaurantId(review.getRestaurantId());
            menuItemReview.setMenuItemId(itemReview.getMenuItemId());
            menuItemReview.setItemName(orderItem.getName());
            menuItemReview.setRating(itemReview.getRating());
            menuItemReview.setComment(itemReview.getComment());
            menuItemReview.setVerified(false);
            return menuItemReview;
        }).toList();
    }

    private ReviewResponseDto buildDetailedResponse(Review review, List<MenuItemReview> itemReviews) {
        ReviewResponseDto responseDto = reviewMapper.toResponseDto(review);
        responseDto.setItemReviews(reviewMapper.toMenuItemResponseDtos(itemReviews));
        return responseDto;
    }

    private void authorizeReviewAccess(Review review, UserPrincipal currentUser) {
        if ("ADMIN".equals(currentUser.getRole())) {
            return;
        }

        if (!review.getCustomerId().equals(currentUser.getUserId())) {
            throw new UnauthorizedActionException("You are not allowed to view this review");
        }
    }

    private void pushRatings(Review review) {
        try {
            restaurantClient.updateRestaurantRating(
                    review.getRestaurantId(),
                    review.getFoodRating().doubleValue()
            );
            log.info("Restaurant rating pushed restaurantId={} rating={}",
                    review.getRestaurantId(), review.getFoodRating());
        } catch (Exception ex) {
            log.error("Failed to push restaurant rating for restaurantId={}", review.getRestaurantId(), ex);
        }

        try {
            deliveryClient.updateDeliveryRating(
                    review.getAgentId(),
                    new DeliveryRatingUpdateRequestDto(review.getDeliveryRating())
            );
            log.info("Delivery rating pushed agentId={} rating={}",
                    review.getAgentId(), review.getDeliveryRating());
        } catch (Exception ex) {
            log.error("Failed to push delivery rating for agentId={}", review.getAgentId(), ex);
        }
    }
}
