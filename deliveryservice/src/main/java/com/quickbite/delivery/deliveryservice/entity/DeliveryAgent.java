package com.quickbite.delivery.deliveryservice.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Table(name = "delivery_agents")
public class DeliveryAgent {
    
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false)
    private String userId;

    @Column(nullable=false)
    private String fullName;

    @Column(nullable=false)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    private VehicleType vehicleType;

    @Column(nullable=false)
    private String vehicleNumber;

    @Column(nullable=false)
    private BigDecimal currentLatitude;

    @Column(nullable=false)
    private BigDecimal currentLongitude;

    @Column(nullable=false)
    private boolean isAvailable = true;

    @Column(nullable=false)
    private boolean isVerified = false;

    @Column(nullable=false)
    private double avgRating = 0;

    @Column(nullable=false)
    private Integer totalDeliveries = 0;

    public DeliveryAgent(String userId, String fullName, String phone, VehicleType vehicleType, String vehicleNumber,
            BigDecimal currentLatitude, BigDecimal currentLongitude) {
        this.userId = userId;
        this.fullName = fullName;
        this.phone = phone;
        this.vehicleType = vehicleType;
        this.vehicleNumber = vehicleNumber;
        this.currentLatitude = currentLatitude;
        this.currentLongitude = currentLongitude;
    }

}
