package com.quickbite.order.orderservice.dto.responseDto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemResponseDto {
    
    private Long orderItemId;

    private Long menuItemId;

    private String name;

    private BigDecimal price;

    private Integer quantity;

    private String customization;
}
