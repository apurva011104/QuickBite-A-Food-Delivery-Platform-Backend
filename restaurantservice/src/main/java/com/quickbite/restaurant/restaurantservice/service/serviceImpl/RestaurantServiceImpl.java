package com.quickbite.restaurant.restaurantservice.service.serviceImpl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.quickbite.restaurant.restaurantservice.dto.requestDto.RestaurantRequestDto;
import com.quickbite.restaurant.restaurantservice.dto.responseDto.OwnerRestaurantDetailsResponseDto;
import com.quickbite.restaurant.restaurantservice.dto.responseDto.RestaurantOrderResponseDto;
import com.quickbite.restaurant.restaurantservice.dto.responseDto.RestaurantOwnerResponseDto;
import com.quickbite.restaurant.restaurantservice.dto.responseDto.RestaurantResponseDto;
import com.quickbite.restaurant.restaurantservice.entity.Restaurant;
import com.quickbite.restaurant.restaurantservice.event.NotificationEvent;
import com.quickbite.restaurant.restaurantservice.external.order.client.OrderClient;
import com.quickbite.restaurant.restaurantservice.mapper.RestaurantMapper;
import com.quickbite.restaurant.restaurantservice.repository.RestaurantRepository;
import com.quickbite.restaurant.restaurantservice.security.UserPrincipal;
import com.quickbite.restaurant.restaurantservice.service.NotificationEventPublisher;
import com.quickbite.restaurant.restaurantservice.service.RestaurantService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class RestaurantServiceImpl implements RestaurantService {

    @Autowired
    private RestaurantRepository repository;

    @Autowired
    private NotificationEventPublisher notificationEventPublisher;

    @Autowired
    private OrderClient orderClient;

    @Override
    @Transactional
    public RestaurantResponseDto registerRestaurant(RestaurantRequestDto request) {
        UserPrincipal user = getCurrentUser();
        validateRestaurantLocation(request, true);

        Restaurant restaurant = RestaurantMapper.mapToEntity(request);
        restaurant.setOwnerId(user.getUserId());
        restaurant.setApproved(false);
        restaurant.setOpen(false);
        restaurant.setRejectionReason(null);

        Restaurant saved = repository.save(restaurant);
        log.info("Restaurant registered. restaurantId={} ownerId={}", saved.getRestaurantId(), user.getUserId());

        // For now send to admin userId = 1
        notificationEventPublisher.publishRestaurantNotification(
                new NotificationEvent(
                        "RESTAURANT_SUBMITTED_FOR_APPROVAL",
                        1L,
                        "Restaurant Approval Needed",
                        "Restaurant '" + saved.getName() + "' is awaiting approval.",
                        saved.getRestaurantId(),
                        "RESTAURANT"
                )
        );

        return RestaurantMapper.mapToDto(saved);
    }

    @Override
    public List<RestaurantResponseDto> getByOwner() {
        UserPrincipal user = getCurrentUser();
        log.debug("Fetching restaurants for ownerId={}", user.getUserId());

        return repository.findByOwnerId(user.getUserId())
                .stream()
                .map(RestaurantMapper::mapToDto)
                .toList();
    }

    @Override
    public OwnerRestaurantDetailsResponseDto getOwnerRestaurantDetails(Long id, String authorizationHeader) {
        UserPrincipal user = getCurrentUser();
        Restaurant restaurant = repository.findByRestaurantIdAndOwnerId(id, user.getUserId())
                .orElseThrow(() -> new RuntimeException("Restaurant not found or access denied"));

        List<RestaurantOrderResponseDto> orders = orderClient.getOrdersByRestaurant(id, authorizationHeader);
        return new OwnerRestaurantDetailsResponseDto(RestaurantMapper.mapToDto(restaurant), orders);
    }

    @Override
    @Transactional
    public RestaurantResponseDto updateRestaurant(Long id, RestaurantRequestDto request) {
        UserPrincipal user = getCurrentUser();

        Restaurant restaurant = repository.findByRestaurantIdAndOwnerId(id, user.getUserId())
                .orElseThrow(() -> new RuntimeException("Restaurant not found or access denied"));

        validateRestaurantLocation(request, false);
        updateEntity(restaurant, request);

        Restaurant updated = repository.save(restaurant);
        log.info("Restaurant updated. restaurantId={} ownerId={}", id, user.getUserId());

        return RestaurantMapper.mapToDto(updated);
    }

    @Override
    @Transactional
    public RestaurantResponseDto toggleOpen(Long id) {
        UserPrincipal user = getCurrentUser();

        Restaurant restaurant = repository.findByRestaurantIdAndOwnerId(id, user.getUserId())
                .orElseThrow(() -> new RuntimeException("Restaurant not found or access denied"));

        if (!restaurant.isApproved()) {
            throw new RuntimeException("Restaurant is not approved yet");
        }

        restaurant.setOpen(!restaurant.isOpen());
        Restaurant updated = repository.save(restaurant);

        log.info("Restaurant open status changed. restaurantId={} ownerId={} isOpen={}",
                id, user.getUserId(), updated.isOpen());

        return RestaurantMapper.mapToDto(updated);
    }

    @Override
    @Transactional
    public void deleteRestaurant(Long id) {
        UserPrincipal user = getCurrentUser();

        Restaurant restaurant = repository.findByRestaurantIdAndOwnerId(id, user.getUserId())
                .orElseThrow(() -> new RuntimeException("Restaurant not found or access denied"));

        repository.delete(restaurant);
        log.info("Restaurant deleted. restaurantId={} ownerId={}", id, user.getUserId());
    }

    @Override
    public RestaurantResponseDto getById(Long id) {
        Restaurant restaurant = repository.findByRestaurantIdAndIsApprovedTrue(id)
                .orElseThrow(() -> new RuntimeException("Restaurant not found"));

        return RestaurantMapper.mapToDto(restaurant);
    }

    @Override
    public List<RestaurantResponseDto> getByCity(String city) {
        return repository.findByCityAndIsApprovedTrue(city)
                .stream()
                .map(RestaurantMapper::mapToDto)
                .toList();
    }

    @Override
    public List<RestaurantResponseDto> getByCuisine(String cuisine) {
        return repository.findByCuisineIgnoreCaseAndIsApprovedTrue(cuisine)
                .stream()
                .map(RestaurantMapper::mapToDto)
                .toList();
    }

    @Override
    public List<RestaurantResponseDto> searchRestaurants(String keyword) {
        return repository.findByNameContainingIgnoreCaseAndIsApprovedTrue(keyword)
                .stream()
                .map(RestaurantMapper::mapToDto)
                .toList();
    }

    @Override
    public List<RestaurantResponseDto> getNearby(Double lat, Double lng, Double radius) {
        List<Restaurant> allApproved = repository.findByIsApprovedTrue();

        return allApproved.stream()
                .filter(r -> r.getLatitude() != null && r.getLongitude() != null)
                .filter(r -> distance(lat, lng, r.getLatitude(), r.getLongitude()) <= radius)
                .map(RestaurantMapper::mapToDto)
                .toList();
    }

    @Override
    @Transactional
    public RestaurantResponseDto approveRestaurant(Long id) {
        Restaurant restaurant = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Restaurant not found"));

        restaurant.setApproved(true);
        restaurant.setRejectionReason(null);

        Restaurant updated = repository.save(restaurant);
        log.info("Restaurant approved. restaurantId={}", id);

        notificationEventPublisher.publishRestaurantNotification(
                new NotificationEvent(
                        "RESTAURANT_APPROVED",
                        updated.getOwnerId(),
                        "Restaurant Approved",
                        "Your restaurant '" + updated.getName() + "' has been approved.",
                        updated.getRestaurantId(),
                        "RESTAURANT"
                )
        );

        return RestaurantMapper.mapToDto(updated);
    }

    @Override
    public List<RestaurantResponseDto> getPendingRestaurants() {
        return repository.findByIsApprovedFalse()
                .stream()
                .map(RestaurantMapper::mapToDto)
                .toList();
    }

    @Override
    @Transactional
    public RestaurantResponseDto rejectRestaurant(Long id, String reason) {
        Restaurant restaurant = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Restaurant not found"));

        restaurant.setApproved(false);
        restaurant.setOpen(false);
        restaurant.setRejectionReason(reason);

        Restaurant updated = repository.save(restaurant);
        log.info("Restaurant rejected. restaurantId={} reason={}", id, reason);

        notificationEventPublisher.publishRestaurantNotification(
                new NotificationEvent(
                        "RESTAURANT_REJECTED",
                        updated.getOwnerId(),
                        "Restaurant Rejected",
                        "Your restaurant '" + updated.getName() + "' was rejected."
                                + (reason != null && !reason.isBlank() ? " Reason: " + reason : ""),
                        updated.getRestaurantId(),
                        "RESTAURANT"
                )
        );
    
        return RestaurantMapper.mapToDto(updated);
    }

    @Override
    @Transactional
    public RestaurantResponseDto updateRating(Long id, Double rating) {
        if (rating == null || rating < 0 || rating > 5) {
            throw new RuntimeException("Rating must be between 0 and 5");
        }

        Restaurant restaurant = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Restaurant not found"));

        long currentCount = restaurant.getRatingCount() == null ? 0L : restaurant.getRatingCount();
        double currentAverage = restaurant.getAvgRating() == null ? 0.0 : restaurant.getAvgRating();

        double newAverage = ((currentAverage * currentCount) + rating) / (currentCount + 1);

        restaurant.setRatingCount(currentCount + 1);
        restaurant.setAvgRating(newAverage);

        Restaurant updated = repository.save(restaurant);
        log.info("Restaurant rating updated. restaurantId={} avgRating={} ratingCount={}",
                id, updated.getAvgRating(), updated.getRatingCount());

        return RestaurantMapper.mapToDto(updated);
    }

    @Override
    public RestaurantOwnerResponseDto getRestaurantOwnerInfo(Long id) {
        Restaurant restaurant = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Restaurant not found"));
        return new RestaurantOwnerResponseDto(
                restaurant.getRestaurantId(),
                restaurant.getOwnerId(),
                restaurant.getName()
        );
    }

    private UserPrincipal getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getPrincipal() == null) {
            throw new RuntimeException("Unauthorized access");
        }

        return (UserPrincipal) authentication.getPrincipal();
    }

    private void updateEntity(Restaurant restaurant, RestaurantRequestDto dto) {
        if (dto.getName() != null) restaurant.setName(dto.getName());
        if (dto.getDescription() != null) restaurant.setDescription(dto.getDescription());
        if (dto.getCuisine() != null) restaurant.setCuisine(dto.getCuisine());
        if (dto.getAddress() != null) restaurant.setAddress(dto.getAddress());
        if (dto.getCity() != null) restaurant.setCity(dto.getCity());
        if (dto.getLatitude() != null) restaurant.setLatitude(dto.getLatitude());
        if (dto.getLongitude() != null) restaurant.setLongitude(dto.getLongitude());
        if (dto.getPhone() != null) restaurant.setPhone(dto.getPhone());
        if (dto.getDeliveryRadius() != null) restaurant.setDeliveryRadius(dto.getDeliveryRadius());
        if (dto.getMinOrderAmount() != null) restaurant.setMinOrderAmount(dto.getMinOrderAmount());
        if (dto.getEstimatedDeliveryMin() != null) restaurant.setEstimatedDeliveryMin(dto.getEstimatedDeliveryMin());
    }

    private void validateRestaurantLocation(RestaurantRequestDto request, boolean locationRequired) {
        Double latitude = request.getLatitude();
        Double longitude = request.getLongitude();

        if (locationRequired && (latitude == null || longitude == null)) {
            throw new RuntimeException("Restaurant location is required");
        }

        if (latitude == null && longitude == null) {
            return;
        }

        if (latitude == null || longitude == null) {
            throw new RuntimeException("Latitude and longitude must be provided together");
        }

        if (latitude < -90 || latitude > 90) {
            throw new RuntimeException("Latitude must be between -90 and 90");
        }

        if (longitude < -180 || longitude > 180) {
            throw new RuntimeException("Longitude must be between -180 and 180");
        }
    }

    private double distance(double lat1, double lon1, double lat2, double lon2) {
        final int radiusOfEarth = 6371;

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return radiusOfEarth * c;
    }
}
