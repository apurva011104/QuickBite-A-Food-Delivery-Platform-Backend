package com.quickbite.order.orderservice.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.quickbite.order.orderservice.dto.requestDto.OrderItemRequestDto;
import com.quickbite.order.orderservice.dto.requestDto.OrderRequestDto;
import com.quickbite.order.orderservice.dto.responseDto.OrderResponseDto;
import com.quickbite.order.orderservice.entity.Order;
import com.quickbite.order.orderservice.entity.OrderItem;
import com.quickbite.order.orderservice.entity.OrderStatus;
import com.quickbite.order.orderservice.entity.PaymentMode;

class MapperTest {

    @Test
    void orderMapperShouldConvertDtoToEntityAndBack() {
        OrderRequestDto request = new OrderRequestDto(
                10L,
                BigDecimal.valueOf(10),
                PaymentMode.COD,
                "221B Baker Street",
                "Less spicy",
                List.of(new OrderItemRequestDto(101L, "Burger", BigDecimal.valueOf(150), 2, "No onion"))
        );

        Order entity = OrderMapper.dtoToEntity(request);
        entity.setOrderId(100L);
        entity.setCustomerId(1L);
        entity.setTotalAmount(BigDecimal.valueOf(300));
        entity.setFinalAmount(BigDecimal.valueOf(290));
        entity.setOrderStatus(OrderStatus.CONFIRMED);
        entity.setOrderDate(LocalDateTime.now());
        entity.setEstimatedDelivery(LocalDateTime.now().plusMinutes(30));
        entity.getItems().get(0).setOrderItemId(1L);

        OrderResponseDto response = OrderMapper.entityToDto(entity);

        assertThat(entity.getItems()).hasSize(1);
        assertThat(entity.getItems().get(0).getOrder()).isEqualTo(entity);
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getFinalAmount()).isEqualByComparingTo("290");
    }

    @Test
    void orderItemMapperShouldConvertEntityAndDto() {
        OrderItemRequestDto request = new OrderItemRequestDto(101L, "Burger", BigDecimal.valueOf(150), 2, "No onion");

        OrderItem entity = OrderItemMapper.dtoToEntity(request);
        entity.setOrderItemId(1L);

        var response = OrderItemMapper.entityToDto(entity);

        assertThat(entity.getMenuItemId()).isEqualTo(101L);
        assertThat(response.getOrderItemId()).isEqualTo(1L);
        assertThat(response.getCustomization()).isEqualTo("No onion");
    }
}
