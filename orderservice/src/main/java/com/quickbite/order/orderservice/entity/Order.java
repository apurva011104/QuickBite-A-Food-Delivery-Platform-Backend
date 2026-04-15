package com.quickbite.order.orderservice.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name="orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId;

    @Column(nullable=false)
    private Long customerId;

    @Column(nullable=false)
    private Long restaurantId;

    @Column(nullable=true)
    private Long deliveryAgentId;

    @Column(nullable=false)
    private BigDecimal totalAmount;

    @Column(nullable=false)
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(nullable=false)
    private BigDecimal finalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    private PaymentMode paymentMode;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    private OrderStatus orderStatus = OrderStatus.PLACED;

    @Column(nullable=false)
    private LocalDateTime orderDate;

    @Column(nullable=false)
    private String deliveryAddress;

    @Column(nullable=false)
    private LocalDateTime estimatedDelivery;

    @Column(nullable=true)
    private String specialInstructions;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }
    
}
