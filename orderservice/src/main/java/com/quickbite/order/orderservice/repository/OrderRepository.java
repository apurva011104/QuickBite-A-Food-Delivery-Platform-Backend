package com.quickbite.order.orderservice.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.quickbite.order.orderservice.entity.Order;
import com.quickbite.order.orderservice.entity.OrderStatus;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByCustomerIdOrderByOrderDateDesc(Long customerId);

    List<Order> findByRestaurantIdOrderByOrderDateDesc(Long restaurantId);

    List<Order> findByOrderStatus(OrderStatus orderStatus);

    List<Order> findByOrderStatusIn(List<OrderStatus> statuses);

    List<Order> findByDeliveryAgentId(Long deliveryAgentId);

    List<Order> findByOrderDateBetween(LocalDateTime start, LocalDateTime end);

    Long countByRestaurantId(Long restaurantId);
}