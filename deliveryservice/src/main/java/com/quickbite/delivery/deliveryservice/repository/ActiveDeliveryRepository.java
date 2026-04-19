package com.quickbite.delivery.deliveryservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.quickbite.delivery.deliveryservice.entity.ActiveDelivery;
import com.quickbite.delivery.deliveryservice.entity.DeliveryStatus;

@Repository
public interface ActiveDeliveryRepository extends JpaRepository<ActiveDelivery, Long> {

    Optional<ActiveDelivery> findByOrderId(Long orderId);

    List<ActiveDelivery> findByAgentIdAndStatusNot(Long agentId, DeliveryStatus status);

    boolean existsByOrderId(Long orderId);
}