package com.quickbite.menu.menuservice.dto.external;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RestaurantSummaryDto {
    private Long restaurantId;
    private String name;
    private String description;
    private String cuisine;
    private String city;
    private Double avgRating;
    private boolean isOpen;
    private boolean isApproved;
    private Integer estimatedDeliveryMin;
}