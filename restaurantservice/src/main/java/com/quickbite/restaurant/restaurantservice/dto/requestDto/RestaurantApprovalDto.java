package com.quickbite.restaurant.restaurantservice.dto.requestDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RestaurantApprovalDto {
    private Long restaurantId;
    private boolean isApproved;
}
