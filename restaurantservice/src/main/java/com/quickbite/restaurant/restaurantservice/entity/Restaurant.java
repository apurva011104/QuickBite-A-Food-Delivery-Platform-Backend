package com.quickbite.restaurant.restaurantservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "restaurants")
@Data
@NoArgsConstructor
public class Restaurant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long restaurantId;

    private Long ownerId;

    private String name;
    private String description;
    private String cuisine;

    private String address;
    private String city;

    private Double latitude;
    private Double longitude;

    private String phone;

    private Double avgRating = 0.0;
    private Long ratingCount = 0L;

    private boolean isOpen = false;
    private boolean isApproved = false;

    @Column(length = 500)
    private String rejectionReason;

    private Double deliveryRadius;
    private Double minOrderAmount;

    private Integer estimatedDeliveryMin;
}