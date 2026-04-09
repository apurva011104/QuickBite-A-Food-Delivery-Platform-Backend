package com.quickbite.restaurant.restaurantservice.service.serviceImpl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.quickbite.restaurant.restaurantservice.dto.requestDto.RestaurantRequestDto;
import com.quickbite.restaurant.restaurantservice.dto.responseDto.RestaurantResponseDto;
import com.quickbite.restaurant.restaurantservice.entity.Restaurant;
import com.quickbite.restaurant.restaurantservice.mapper.RestaurantMapper;
import com.quickbite.restaurant.restaurantservice.repository.RestaurantRepository;
import com.quickbite.restaurant.restaurantservice.security.UserPrincipal;
import com.quickbite.restaurant.restaurantservice.service.RestaurantService;

@Service
public class RestaurantServiceImpl implements RestaurantService {

    @Autowired
    private RestaurantRepository repository;

    //-----------------Owner methods-----------------

    @Override
    public RestaurantResponseDto registerRestaurant(RestaurantRequestDto request) {

        UserPrincipal user = getCurrentUser();

        Restaurant r = RestaurantMapper.mapToEntity(request);
        r.setOwnerId(user.getUserId());
        r.setApproved(false);
        r.setOpen(false);

        return RestaurantMapper.mapToDto(repository.save(r));
    }

    @Override
    public List<RestaurantResponseDto> getByOwner() {

        UserPrincipal user = getCurrentUser();

        return repository.findByOwnerId(user.getUserId())
                .stream()
                .map(RestaurantMapper::mapToDto)
                .toList();
    }

    @Override
    public RestaurantResponseDto updateRestaurant(Long id, RestaurantRequestDto request) {

        UserPrincipal user = getCurrentUser();

        Restaurant r = repository.findByRestaurantIdAndOwnerId(id, user.getUserId())
                .orElseThrow(() -> new RuntimeException("Unauthorized or not found"));

        updateEntity(r, request);

        return RestaurantMapper.mapToDto(repository.save(r));
    }

    @Override
    public RestaurantResponseDto toggleOpen(Long id) {

        UserPrincipal user = getCurrentUser();

        Restaurant r = repository.findByRestaurantIdAndOwnerId(id, user.getUserId())
                .orElseThrow(() -> new RuntimeException("Unauthorized or not found"));

        r.setOpen(!r.isOpen());

        return RestaurantMapper.mapToDto(repository.save(r));
    }

    @Override
    public void deleteRestaurant(Long id) {

        UserPrincipal user = getCurrentUser();

        Restaurant r = repository.findByRestaurantIdAndOwnerId(id, user.getUserId())
                .orElseThrow(() -> new RuntimeException("Unauthorized or not found"));

        repository.delete(r);
    }

    //-----------------Public methods-----------------

    @Override
    public RestaurantResponseDto getById(Long id) {

        Restaurant r = repository.findByRestaurantIdAndIsApprovedTrue(id)
                .orElseThrow(() -> new RuntimeException("Restaurant not found"));

        return RestaurantMapper.mapToDto(r);
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

        List<Restaurant> all = repository.findByIsApprovedTrue();

        return all.stream()
                .filter(r -> distance(lat, lng, r.getLatitude(), r.getLongitude()) <= radius)
                .map(RestaurantMapper::mapToDto)
                .toList();
    }

    //-----------------Admin methhods-----------------

    @Override
    public RestaurantResponseDto approveRestaurant(Long id) {

        Restaurant r = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Restaurant not found"));

        r.setApproved(true);

        return RestaurantMapper.mapToDto(repository.save(r));
    }

    //-----------------System methods-----------------

    @Override
    public RestaurantResponseDto updateRating(Long id, Double rating) {

        Restaurant r = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Restaurant not found"));

        double newRating = (r.getAvgRating() + rating) / 2;
        r.setAvgRating(newRating);

        return RestaurantMapper.mapToDto(repository.save(r));
    }

    //-----------------Helper methods-----------------

    private UserPrincipal getCurrentUser() {
        return (UserPrincipal) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();
    }

    private void updateEntity(Restaurant restaurant, RestaurantRequestDto dto) {

        restaurant.setName(dto.getName());
        restaurant.setDescription(dto.getDescription());
        restaurant.setCuisine(dto.getCuisine());
        restaurant.setAddress(dto.getAddress());
        restaurant.setCity(dto.getCity());
        restaurant.setLatitude(dto.getLatitude());
        restaurant.setLongitude(dto.getLongitude());
        restaurant.setPhone(dto.getPhone());
        restaurant.setDeliveryRadius(dto.getDeliveryRadius());
        restaurant.setMinOrderAmount(dto.getMinOrderAmount());
        restaurant.setEstimatedDeliveryMin(dto.getEstimatedDeliveryMin());
    }

    private double distance(double lat1, double lon1, double lat2, double lon2) {

        final int R = 6371; //Earth radius in kilometers

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }
}
