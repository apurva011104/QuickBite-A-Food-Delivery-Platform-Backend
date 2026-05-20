package com.quickbite.order.orderservice.service.serviceImpl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.quickbite.order.orderservice.dto.requestDto.OrderItemRequestDto;
import com.quickbite.order.orderservice.dto.requestDto.OrderRequestDto;
import com.quickbite.order.orderservice.dto.requestDto.PaymentRequestDto;
import com.quickbite.order.orderservice.dto.responseDto.OrderResponseDto;
import com.quickbite.order.orderservice.dto.responseDto.PaymentResponseDto;
import com.quickbite.order.orderservice.dto.responseDto.PaymentStatus;
import com.quickbite.order.orderservice.dto.responseDto.RestaurantOwnerResponseDto;
import com.quickbite.order.orderservice.entity.Order;
import com.quickbite.order.orderservice.entity.OrderStatus;
import com.quickbite.order.orderservice.entity.PaymentMode;
import com.quickbite.order.orderservice.event.NotificationEvent;
import com.quickbite.order.orderservice.exception.EmptyOrderException;
import com.quickbite.order.orderservice.exception.InvalidOrderStateException;
import com.quickbite.order.orderservice.exception.OrderNotFoundException;
import com.quickbite.order.orderservice.exception.UnauthorizedActionException;
import com.quickbite.order.orderservice.external.payment.client.PaymentClient;
import com.quickbite.order.orderservice.external.restaurant.client.RestaurantClient;
import com.quickbite.order.orderservice.mapper.OrderMapper;
import com.quickbite.order.orderservice.repository.OrderRepository;
import com.quickbite.order.orderservice.security.UserPrincipal;
import com.quickbite.order.orderservice.service.NotificationEventPublisher;
import com.quickbite.order.orderservice.service.OrderService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentClient paymentClient;

    @Autowired
    private RestaurantClient restaurantClient;

    @Autowired
    private NotificationEventPublisher notificationEventPublisher;

    public OrderServiceImpl(OrderRepository orderRepository,
                            PaymentClient paymentClient,
                            RestaurantClient restaurantClient) {
        this.orderRepository = orderRepository;
        this.paymentClient = paymentClient;
        this.restaurantClient = restaurantClient;
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
        notificationEventPublisher.publishOrderNotification(
                new NotificationEvent(
                        "ORDER_PLACED",
                        savedOrder.getCustomerId(),
                        "Order Placed",
                        "Your order #" + savedOrder.getOrderId() + " has been placed successfully.",
                        savedOrder.getOrderId(),
                        "ORDER"
                )
        );

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
                    savedOrder.setOrderStatus(OrderStatus.PLACED);
                    log.info("COD order placed and awaiting restaurant confirmation orderId={}",
                            savedOrder.getOrderId());
                } else if ((savedOrder.getPaymentMode() == PaymentMode.CARD
                        || savedOrder.getPaymentMode() == PaymentMode.UPI)
                        && paymentResponse.getStatus() == PaymentStatus.PENDING) {
                    savedOrder.setOrderStatus(OrderStatus.PAYMENT_PENDING);
                    log.info("Online payment initialized orderId={}, awaiting Razorpay verification",
                            savedOrder.getOrderId());
                } else if (paymentResponse.getStatus() == PaymentStatus.PAID) {
                    savedOrder.setOrderStatus(OrderStatus.PLACED);
                    log.info("Online payment successful and order is awaiting restaurant confirmation orderId={}",
                            savedOrder.getOrderId());

                } else {
                    savedOrder.setOrderStatus(OrderStatus.CANCELLED);
                    log.warn("Payment failed/pending for non-COD orderId={}", savedOrder.getOrderId());
                    notificationEventPublisher.publishOrderNotification(
                            new NotificationEvent(
                                    "ORDER_CANCELLED",
                                    savedOrder.getCustomerId(),
                                    "Order Cancelled",
                                    "Your order #" + savedOrder.getOrderId() + " has been cancelled.",
                                    savedOrder.getOrderId(),
                                    "ORDER"
                            )
                    );
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
        if (updated.getOrderStatus() != OrderStatus.CANCELLED) {
            publishOwnerOrderReceivedNotification(updated);
        }
        return OrderMapper.entityToDto(updated);
    }

    @Override
    public OrderResponseDto getOrderById(Long orderId, UserPrincipal currentUser, String token) {
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

        order = synchronizeOnlinePaymentStatus(order, token);
    
        return OrderMapper.entityToDto(order);
    }

    @Override
    public List<OrderResponseDto> getOrdersByCustomer(Long customerId, String token) {
        return orderRepository.findByCustomerIdOrderByOrderDateDesc(customerId)
                .stream()
                .map(order -> synchronizeOnlinePaymentStatus(order, token))
                .map(OrderMapper::entityToDto)
            .toList();
    }

    @Override
    public List<OrderResponseDto> getOrdersByRestaurant(Long restaurantId, UserPrincipal currentUser, String token) {
        validateRestaurantOwnerAccess(restaurantId, currentUser);

        return orderRepository.findByRestaurantIdOrderByOrderDateDesc(restaurantId)
                .stream()
                .map(order -> synchronizeOnlinePaymentStatus(order, token))
                .map(OrderMapper::entityToDto)
                .toList();
    }

    @Override
    public List<OrderResponseDto> getActiveOrders() {
        List<OrderStatus> activeStatuses = List.of(
                OrderStatus.PLACED,
                OrderStatus.PAYMENT_PENDING,
                OrderStatus.CONFIRMED,
                OrderStatus.PREPARING,
                OrderStatus.READY_FOR_PICKUP,
                OrderStatus.OUT_FOR_DELIVERY,
                OrderStatus.PICKED_UP
        );

        return orderRepository.findByOrderStatusIn(activeStatuses)
                .stream()
                .map(OrderMapper::entityToDto)
                .toList();
    }

    @Override
    @Transactional
    public OrderResponseDto updateOrderStatus(Long orderId, OrderStatus newStatus, UserPrincipal currentUser, String token) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + orderId));

        validateStatusUpdateAccess(order, newStatus, currentUser);
        validateStatusTransition(order.getOrderStatus(), newStatus);

        OrderStatus previousStatus = order.getOrderStatus();
        order.setOrderStatus(newStatus);
        Order updated = orderRepository.save(order);

        log.info("Order status updated orderId={} from={} to={}",
                orderId, previousStatus, newStatus);

        handleOrderStatusSideEffects(updated, previousStatus, currentUser, token);

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

        if (!(order.getOrderStatus() == OrderStatus.PLACED
                || order.getOrderStatus() == OrderStatus.PAYMENT_PENDING)) {
            throw new InvalidOrderStateException("Order cannot be cancelled after the restaurant confirms it");
        }

        order.setOrderStatus(OrderStatus.CANCELLED);
        Order updated = orderRepository.save(order);

        log.info("Order cancelled orderId={} customerId={}", orderId, currentUser.getUserId());
        notificationEventPublisher.publishOrderNotification(
                new NotificationEvent(
                        "ORDER_CANCELLED",
                        updated.getCustomerId(),
                        "Order Cancelled",
                        "Your order #" + updated.getOrderId() + " has been cancelled.",
                        updated.getOrderId(),
                        "ORDER"
                )
        );
        requestRefundIfRequired(order, token);

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
        request.setDeliveryLatitude(oldOrder.getDeliveryLatitude());
        request.setDeliveryLongitude(oldOrder.getDeliveryLongitude());
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
        if (currentStatus == OrderStatus.CANCELLED
                || currentStatus == OrderStatus.REJECTED
                || currentStatus == OrderStatus.DELIVERED) {
            throw new InvalidOrderStateException("No further status change allowed from " + currentStatus);
        }

        boolean valid = switch (currentStatus) {
            case PLACED -> newStatus == OrderStatus.CONFIRMED || newStatus == OrderStatus.REJECTED;
            case PAYMENT_PENDING -> newStatus == OrderStatus.PLACED || newStatus == OrderStatus.CANCELLED;
            case CONFIRMED -> newStatus == OrderStatus.PREPARING;
            case PREPARING -> newStatus == OrderStatus.READY_FOR_PICKUP;
            case READY_FOR_PICKUP -> newStatus == OrderStatus.OUT_FOR_DELIVERY;
            case OUT_FOR_DELIVERY -> newStatus == OrderStatus.DELIVERED;
            case PICKED_UP -> newStatus == OrderStatus.DELIVERED;
            default -> false;
        };

        if (!valid) {
            throw new InvalidOrderStateException(
                    "Invalid status transition from " + currentStatus + " to " + newStatus
            );
        }
    }

    private Order synchronizeOnlinePaymentStatus(Order order, String token) {
        if (order.getOrderStatus() != OrderStatus.PAYMENT_PENDING) {
            return order;
        }

        if (order.getPaymentMode() != PaymentMode.CARD && order.getPaymentMode() != PaymentMode.UPI) {
            return order;
        }

        try {
            PaymentResponseDto payment = paymentClient.getPaymentByOrder(order.getOrderId(), "Bearer " + token);
            if (payment != null && payment.getStatus() == PaymentStatus.PAID) {
                order.setOrderStatus(OrderStatus.PLACED);
                Order updated = orderRepository.save(order);
                log.info("Online payment verified and order is awaiting restaurant confirmation orderId={}",
                        updated.getOrderId());
                return updated;
            }
        } catch (Exception ex) {
            log.warn("Unable to synchronize payment status for orderId={}", order.getOrderId(), ex);
        }

        return order;
    }

    private void validateRestaurantOwnerAccess(Long restaurantId, UserPrincipal currentUser) {
        if ("ADMIN".equals(currentUser.getRole())) {
            return;
        }

        RestaurantOwnerResponseDto restaurant = restaurantClient.getOwnerInfo(restaurantId);
        if (restaurant == null || !currentUser.getUserId().equals(restaurant.getOwnerId())) {
            throw new UnauthorizedActionException("You are not allowed to view orders for this restaurant");
        }
    }

    private void validateStatusUpdateAccess(Order order, OrderStatus newStatus, UserPrincipal currentUser) {
        if ("ADMIN".equals(currentUser.getRole()) || "AGENT".equals(currentUser.getRole())) {
            return;
        }

        if (!"OWNER".equals(currentUser.getRole())) {
            throw new UnauthorizedActionException("You are not allowed to update this order");
        }

        validateRestaurantOwnerAccess(order.getRestaurantId(), currentUser);

        if (newStatus != OrderStatus.CONFIRMED
                && newStatus != OrderStatus.PREPARING
                && newStatus != OrderStatus.READY_FOR_PICKUP
                && newStatus != OrderStatus.REJECTED) {
            throw new UnauthorizedActionException(
                    "Owners can only move orders to CONFIRMED, PREPARING, READY_FOR_PICKUP, or REJECTED"
            );
        }
    }

    private void handleOrderStatusSideEffects(Order updated,
                                              OrderStatus previousStatus,
                                              UserPrincipal currentUser,
                                              String token) {
        if (!"OWNER".equals(currentUser.getRole()) && !"ADMIN".equals(currentUser.getRole())) {
            return;
        }

        if (updated.getOrderStatus() == OrderStatus.CONFIRMED) {
            notificationEventPublisher.publishOrderNotification(
                    new NotificationEvent(
                            "ORDER_CONFIRMED",
                            updated.getCustomerId(),
                            "Order Confirmed",
                            "Your order #" + updated.getOrderId() + " has been confirmed by the restaurant.",
                            updated.getOrderId(),
                            "ORDER"
                    )
            );
            return;
        }

        if (updated.getOrderStatus() != OrderStatus.REJECTED) {
            return;
        }

        log.info("Order rejected by restaurant orderId={} previousStatus={} actorRole={} actorId={}",
                updated.getOrderId(), previousStatus, currentUser.getRole(), currentUser.getUserId());

        notificationEventPublisher.publishOrderNotification(
                new NotificationEvent(
                        "ORDER_REJECTED",
                        updated.getCustomerId(),
                        "Order Rejected",
                        "Your order #" + updated.getOrderId() + " was rejected by the restaurant.",
                        updated.getOrderId(),
                        "ORDER"
                )
        );

        requestRefundIfRequired(updated, token);
    }

    private void requestRefundIfRequired(Order order, String token) {
        if (order.getPaymentMode() == PaymentMode.COD) {
            return;
        }

        try {
            paymentClient.refundPayment(order.getOrderId(), "Bearer " + token);
            log.info("Refund requested for orderId={}", order.getOrderId());
        } catch (Exception ex) {
            log.error("Refund call failed for orderId={}", order.getOrderId(), ex);
        }
    }

    private void publishOwnerOrderReceivedNotification(Order order) {
        try {
            RestaurantOwnerResponseDto restaurant = restaurantClient.getOwnerInfo(order.getRestaurantId());
            if (restaurant == null || restaurant.getOwnerId() == null) {
                log.warn("Skipping owner notification because restaurant owner was not resolved for restaurantId={}",
                        order.getRestaurantId());
                return;
            }

            notificationEventPublisher.publishOrderNotification(
                    new NotificationEvent(
                            "ORDER_RECEIVED",
                            restaurant.getOwnerId(),
                            "New Order Received",
                            "You have received a new order #" + order.getOrderId()
                                    + " for restaurant '" + restaurant.getName() + "'.",
                            order.getOrderId(),
                            "ORDER"
                    )
            );
            log.info("Owner notified for orderId={} ownerId={} restaurantId={}",
                    order.getOrderId(), restaurant.getOwnerId(), order.getRestaurantId());
        } catch (Exception ex) {
            log.warn("Unable to notify restaurant owner for orderId={} restaurantId={}",
                    order.getOrderId(), order.getRestaurantId(), ex);
        }
    }
}
