package com.quickbite.menu.menuservice.dto.requestDto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryRequestDto {

    @NotNull
    private Long restaurantId;

    @NotNull
    private String name;

    private String description;

    private String imageUrl;

    @NotNull
    private int displayOrder;
}