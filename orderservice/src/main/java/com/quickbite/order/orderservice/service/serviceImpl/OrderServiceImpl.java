package com.quickbite.order.orderservice.service.serviceImpl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.quickbite.order.orderservice.dto.requestDto.OrderItemRequestDto;
import com.quickbite.order.orderservice.dto.requestDto.OrderRequestDto;
import com.quickbite.order.orderservice.dto.requestDto.PaymentRequestDto;
import com.quickbite.order.orderservice.dto.responseDto.OrderResponseDto;
import com.quickbite.order.orderservice.dto.responseDto.PaymentResponseDto;
import com.quickbite.order.orderservice.dto.responseDto.PaymentStatus;
import com.quickbite.order.orderservice.entity.Order;
import com.quickbite.order.orderservice.entity.OrderStatus;
import com.quickbite.order.orderservice.entity.PaymentMode;
import com.quickbite.order.orderservice.exception.EmptyOrderException;
import com.quickbite.order.orderservice.exception.InvalidOrderStateException;
import com.quickbite.order.orderservice.exception.OrderNotFoundException;
import com.quickbite.order.orderservice.exception.UnauthorizedActionException;
import com.quickbite.order.orderservice.external.payment.client.PaymentClient;
import com.quickbite.order.orderservice.mapper.OrderMapper;
import com.quickbite.order.orderservice.repository.OrderRepository;
import com.quickbite.order.orderservice.security.UserPrincipal;
import com.quickbite.order.orderservice.service.OrderService;

@Service
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final OrderRepository orderRepository;
    private final PaymentClient paymentClient;

    public OrderServiceImpl(OrderRepository orderRepository,
                            PaymentClient paymentClient) {
        this.orderRepository = orderRepository;
        this.paymentClient = paymentClient;
    }

    @Override
    @Transactional
    public OrderResponseDto placeOrder(OrderRequestDto request, UserPrincipal currentUser, String token) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new EmptyOrderException("Order must contain at least one item");
        }

        Long customerId = currentUser.getUserId();
        log.info("Placing order for customerId={} restaurantId={}", customerId, request.getRestaurantId());

        Order order = OrderMapper.dtoToEntity(request);
        order.setCustomerId(customerId);
        order.setOrderStatus(OrderStatus.PLACED);
        order.setOrderDate(LocalDateTime.now());
        order.setEstimatedDelivery(LocalDateTime.now().plusMinutes(30));

        BigDecimal totalAmount = order.getItems().stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        order.setTotalAmount(totalAmount);

        BigDecimal discount = request.getDiscount() != null ? request.getDiscount() : BigDecimal.ZERO;
        if (discount.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidOrderStateException("Discount cannot be negative");
        }

        BigDecimal finalAmount = totalAmount.subtract(discount);
        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            finalAmount = BigDecimal.ZERO;
        }

        order.setDiscount(discount);
        order.setFinalAmount(finalAmount);

        Order savedOrder = orderRepository.save(order);
        log.info("Order created orderId={} status={}", savedOrder.getOrderId(), savedOrder.getOrderStatus());

        PaymentRequestDto paymentRequest = new PaymentRequestDto();
        paymentRequest.setOrderId(savedOrder.getOrderId());
        paymentRequest.setAmount(savedOrder.getFinalAmount());
        paymentRequest.setMode(savedOrder.getPaymentMode());

        try {
            PaymentResponseDto paymentResponse = paymentClient.processPayment(
                    paymentRequest,
                    "Bearer " + token
            );

            if (paymentResponse != null) {
                if (savedOrder.getPaymentMode() == PaymentMode.COD
                        && paymentResponse.getStatus() == PaymentStatus.PENDING) {
                    savedOrder.setOrderStatus(OrderStatus.CONFIRMED);
                    log.info("COD order confirmed orderId={}", savedOrder.getOrderId());
                } else if (paymentResponse.getStatus() == PaymentStatus.PAID) {
                    savedOrder.setOrderStatus(OrderStatus.CONFIRMED);
                    log.info("Online payment successful orderId={}", savedOrder.getOrderId());
                } else {
                    savedOrder.setOrderStatus(OrderStatus.CANCELLED);
                    log.warn("Payment failed/pending for non-COD orderId={}", savedOrder.getOrderId());
                }
            } else {
                savedOrder.setOrderStatus(OrderStatus.CANCELLED);
                log.warn("Payment service returned null for orderId={}", savedOrder.getOrderId());
            }
        } catch (Exception ex) {
            savedOrder.setOrderStatus(OrderStatus.CANCELLED);
            log.error("Payment service call failed for orderId={}", savedOrder.getOrderId(), ex);
        }

        Order updated = orderRepository.save(savedOrder);
        return OrderMapper.entityToDto(updated);
    }

    @Override
    public OrderResponseDto getOrderById(Long orderId, UserPrincipal currentUser) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + orderId));
    
        if ("CUSTOMER".equals(currentUser.getRole()) && !order.getCustomerId().equals(currentUser.getUserId())) {
            throw new UnauthorizedActionException("You are not allowed to view this order");
        }
    
        if ("AGENT".equals(currentUser.getRole())
                && order.getDeliveryAgentId() != null
                && !order.getDeliveryAgentId().equals(currentUser.getUserId())) {
            throw new UnauthorizedActionException("You are not allowed to view this order");
        }
    
        return OrderMapper.entityToDto(order);
    }

    @Override
    public List<OrderResponseDto> getOrdersByCustomer(Long customerId) {
        return orderRepository.findByCustomerIdOrderByOrderDateDesc(customerId)
                .stream()
                .map(OrderMapper::entityToDto)
            .toList();
    }

    @Override
    public List<OrderResponseDto> getOrdersByRestaurant(Long restaurantId) {
        return orderRepository.findByRestaurantIdOrderByOrderDateDesc(restaurantId)
                .stream()
                .map(OrderMapper::entityToDto)
                .toList();
    }

    @Override
    public List<OrderResponseDto> getActiveOrders() {
        List<OrderStatus> activeStatuses = List.of(
                OrderStatus.PLACED,
                OrderStatus.CONFIRMED,
                OrderStatus.PREPARING,
                OrderStatus.PICKED_UP
        );

        return orderRepository.findByOrderStatusIn(activeStatuses)
                .stream()
                .map(OrderMapper::entityToDto)
                .toList();
    }

    @Override
    @Transactional
    public OrderResponseDto updateOrderStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + orderId));

        validateStatusTransition(order.getOrderStatus(), newStatus);

        order.setOrderStatus(newStatus);
        Order updated = orderRepository.save(order);

        log.info("Order status updated orderId={} from={} to={}",
                orderId, order.getOrderStatus(), newStatus);

        return OrderMapper.entityToDto(updated);
    }

    @Override
    @Transactional
    public OrderResponseDto assignDeliveryAgent(Long orderId, Long agentId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + orderId));

        order.setDeliveryAgentId(agentId);
        Order updated = orderRepository.save(order);

        log.info("Delivery agent assigned orderId={} agentId={}", orderId, agentId);
        return OrderMapper.entityToDto(updated);
    }

    @Override
    @Transactional
    public OrderResponseDto cancelOrder(Long orderId, UserPrincipal currentUser,String token) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + orderId));

        if (!order.getCustomerId().equals(currentUser.getUserId())) {
            throw new UnauthorizedActionException("You are not allowed to cancel this order");
        }

        if (!(order.getOrderStatus() == OrderStatus.PLACED || order.getOrderStatus() == OrderStatus.CONFIRMED)) {
            throw new InvalidOrderStateException("Order cannot be cancelled after preparation begins");
        }

        order.setOrderStatus(OrderStatus.CANCELLED);
        Order updated = orderRepository.save(order);

        log.info("Order cancelled orderId={} customerId={}", orderId, currentUser.getUserId());

        if (order.getPaymentMode() != PaymentMode.COD) {
            try {
                paymentClient.refundPayment(orderId, "Bearer" + token);
                log.info("Refund requested for orderId={}", orderId);
            } catch (Exception ex) {
                log.error("Refund call failed for orderId={}", orderId, ex);
            }
        }

        return OrderMapper.entityToDto(updated);
    }

    @Override
    public OrderResponseDto reorderFromHistory(Long orderId, UserPrincipal currentUser, String token) {
        Order oldOrder = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + orderId));

        if (!oldOrder.getCustomerId().equals(currentUser.getUserId())) {
            throw new UnauthorizedActionException("You are not allowed to reorder this order");
        }

        OrderRequestDto request = new OrderRequestDto();
        request.setRestaurantId(oldOrder.getRestaurantId());
        request.setDiscount(BigDecimal.ZERO);
        request.setPaymentMode(oldOrder.getPaymentMode());
        request.setDeliveryAddress(oldOrder.getDeliveryAddress());
        request.setSpecialInstructions(oldOrder.getSpecialInstructions());

        List<OrderItemRequestDto> items = oldOrder.getItems().stream().map(item ->
                new OrderItemRequestDto(
                        item.getMenuItemId(),
                        item.getName(),
                        item.getPrice(),
                        item.getQuantity(),
                        item.getCustomization()
                )
        ).toList();

        request.setItems(items);

        log.info("Reordering old orderId={} for customerId={}", orderId, currentUser.getUserId());
        return placeOrder(request, currentUser, token);
    }

    @Override
    public Long getOrderCountByRestaurant(Long restaurantId) {
        return orderRepository.countByRestaurantId(restaurantId);
    }

    private void validateStatusTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        if (currentStatus == OrderStatus.CANCELLED || currentStatus == OrderStatus.DELIVERED) {
            throw new InvalidOrderStateException("No further status change allowed from " + currentStatus);
        }

        boolean valid = switch (currentStatus) {
            case PLACED -> newStatus == OrderStatus.CONFIRMED || newStatus == OrderStatus.CANCELLED;
            case CONFIRMED -> newStatus == OrderStatus.PREPARING || newStatus == OrderStatus.CANCELLED;
            case PREPARING -> newStatus == OrderStatus.PICKED_UP;
            case PICKED_UP -> newStatus == OrderStatus.DELIVERED;
            default -> false;
        };

        if (!valid) {
            throw new InvalidOrderStateException(
                    "Invalid status transition from " + currentStatus + " to " + newStatus
            );
        }
    }
}