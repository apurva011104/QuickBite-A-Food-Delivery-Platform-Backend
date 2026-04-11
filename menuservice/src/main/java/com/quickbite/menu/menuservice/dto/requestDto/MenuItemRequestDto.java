package com.quickbite.menu.menuservice.dto.requestDto;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MenuItemRequestDto {

    @NotNull
    private Long restaurantId;

    @NotNull
    private Long categoryId;

    @NotNull
    private String name;

    private String description;

    @NotNull
    private BigDecimal price;

    @NotNull
    private BigDecimal discountedPrice;

    private String imageUrl;

    private boolean isVeg;

    @Positive
    private double calories;

    @NotNull
    private List<String> tags;
}
