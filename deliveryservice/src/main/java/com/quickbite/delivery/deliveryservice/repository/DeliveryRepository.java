package com.quickbite.delivery.deliveryservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.quickbite.delivery.deliveryservice.entity.DeliveryAgent;

@Repository
public interface DeliveryRepository extends JpaRepository<DeliveryAgent, Long> {

    Optional<DeliveryAgent> findByAgentId(Long agentId);

    Optional<DeliveryAgent> findByUserId(Long userId);

    List<DeliveryAgent> findByAvailableTrue();

    List<DeliveryAgent> findByVerifiedTrue();

    List<DeliveryAgent> findByVerifiedFalseOrderByCreatedAtAsc();

    List<DeliveryAgent> findByAvailableTrueAndVerifiedTrue();

    long countByAvailableTrue();

    void deleteByAgentId(Long agentId);

    boolean existsByUserId(Long userId);

    boolean existsByPhone(String phone);

    boolean existsByVehicleNumber(String vehicleNumber);
}
