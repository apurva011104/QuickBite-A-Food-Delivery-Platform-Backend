package com.quickbite.restaurant.restaurantservice.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.quickbite.restaurant.restaurantservice.dto.requestDto.RestaurantApprovalRequestDto;
import com.quickbite.restaurant.restaurantservice.dto.requestDto.RestaurantRequestDto;
import com.quickbite.restaurant.restaurantservice.dto.responseDto.RestaurantResponseDto;
import com.quickbite.restaurant.restaurantservice.service.RestaurantService;

@RestController
@RequestMapping("/restaurants")
public class RestaurantController {
    
    @Autowired
    private RestaurantService service;

    //-----------------Owner endpoints-----------------
    //Register Restaurant
    @PostMapping("/owner/register")
    public ResponseEntity<RestaurantResponseDto> register(
            @RequestBody RestaurantRequestDto request) {

        return ResponseEntity.ok(service.registerRestaurant(request));
    }

    //Get My Restaurants
    @GetMapping("/owner/my")
    public ResponseEntity<List<RestaurantResponseDto>> getMyRestaurants() {
        return ResponseEntity.ok(service.getByOwner());
    }

    //Update Restaurant
    @PutMapping("/owner/update/{id}")
    public ResponseEntity<RestaurantResponseDto> update(
            @PathVariable Long id,
            @RequestBody RestaurantRequestDto request) {

        return ResponseEntity.ok(service.updateRestaurant(id, request));
    }

    //Toggle Open/Close
    @PatchMapping("/owner/toggle/{id}")
    public ResponseEntity<RestaurantResponseDto> toggle(@PathVariable Long id) {
        return ResponseEntity.ok(service.toggleOpen(id));
    }

    //Delete Restaurant
    @DeleteMapping("/owner/delete/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        service.deleteRestaurant(id);
        return ResponseEntity.ok("Restaurant deleted successfully");
    }

    //-----------------Public endpoints-----------------

    //Get by ID
    @GetMapping("/public/{id}")
    public ResponseEntity<RestaurantResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    //Get by City
    @GetMapping("/public/city/{city}")
    public ResponseEntity<List<RestaurantResponseDto>> getByCity(@PathVariable String city) {
        return ResponseEntity.ok(service.getByCity(city));
    }

    //Get by Cuisine
    @GetMapping("/public/cuisine/{cuisine}")
    public ResponseEntity<List<RestaurantResponseDto>> getByCuisine(@PathVariable String cuisine) {
        return ResponseEntity.ok(service.getByCuisine(cuisine));
    }

    //Search
    @GetMapping("/public/search")
    public ResponseEntity<List<RestaurantResponseDto>> search(
            @RequestParam String keyword) {

        return ResponseEntity.ok(service.searchRestaurants(keyword));
    }

    //Nearby Restaurants
    @GetMapping("/public/nearby")
    public ResponseEntity<List<RestaurantResponseDto>> getNearby(
            @RequestParam Double lat,
            @RequestParam Double lng,
            @RequestParam Double radius) {

        return ResponseEntity.ok(service.getNearby(lat, lng, radius));
    }

    //-----------------Admin endpoints-----------------
    //Approve Restaurant
    @PutMapping("/admin/approve/{id}")
    public ResponseEntity<RestaurantResponseDto> approve(@PathVariable Long id) {
        return ResponseEntity.ok(service.approveRestaurant(id));
    }

    //Get Pending Restaurants
    @GetMapping("/admin/pending")
    public ResponseEntity<List<RestaurantResponseDto>> getPendingRestaurants() {
        return ResponseEntity.ok(service.getPendingRestaurants());
    }

    //Reject Restaurant
    @PutMapping("/admin/reject/{id}")
    public ResponseEntity<RestaurantResponseDto> reject(@PathVariable Long id,
                                                        @RequestBody RestaurantApprovalRequestDto request) {
        return ResponseEntity.ok(service.rejectRestaurant(id, request.getReason()));
    }

    //-----------------System endpoints-----------------
    //Update Rating
    @PutMapping("/internal/rating/{id}")
    public ResponseEntity<RestaurantResponseDto> updateRating(
            @PathVariable Long id,
            @RequestParam Double rating) {

        return ResponseEntity.ok(service.updateRating(id, rating));
    }
}
