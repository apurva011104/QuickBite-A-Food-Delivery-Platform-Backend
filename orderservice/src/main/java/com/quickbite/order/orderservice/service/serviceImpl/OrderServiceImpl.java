package com.quickbite.order.orderservice.service.serviceImpl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.quickbite.order.orderservice.dto.requestDto.OrderRequestDto;
import com.quickbite.order.orderservice.dto.responseDto.OrderResponseDto;
import com.quickbite.order.orderservice.entity.Order;
import com.quickbite.order.orderservice.entity.OrderItem;
import com.quickbite.order.orderservice.entity.OrderStatus;
import com.quickbite.order.orderservice.exception.OrderNotFoundException;
import com.quickbite.order.orderservice.exception.UnauthorizedActionException;
import com.quickbite.order.orderservice.mapper.OrderMapper;
import com.quickbite.order.orderservice.repository.OrderRepository;
import com.quickbite.order.orderservice.service.OrderService;
import com.quickbite.order.orderservice.util.JwtUtil;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private JwtUtil jwtUtil;

    //PLACE ORDER
    @Override
    public OrderResponseDto placeOrder(OrderRequestDto request, String token) {

        Long customerId = jwtUtil.extractUserId(token);

        Order order = OrderMapper.dtoToEntity(request);

        order.setCustomerId(customerId);
        order.setOrderDate(LocalDateTime.now());
        order.setEstimatedDelivery(LocalDateTime.now().plusMinutes(30));

        BigDecimal totalAmount = order.getItems().stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        order.setTotalAmount(totalAmount);

        BigDecimal discount = order.getDiscount() != null ? order.getDiscount() : BigDecimal.ZERO;

        BigDecimal finalAmount = totalAmount.subtract(discount);
        order.setFinalAmount(finalAmount);

        Order savedOrder = orderRepository.save(order);

        return OrderMapper.entityToDto(savedOrder);
    }

    //GET ORDER BY ID
    @Override
    public OrderResponseDto getOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: "+orderId));

        return OrderMapper.entityToDto(order);
    }

    //GET ORDERS BY CUSTOMER
    @Override
    public List<OrderResponseDto> getOrdersByCustomer(String token) {

        Long customerId = jwtUtil.extractUserId(token);

        return orderRepository.findByCustomerIdOrderByOrderDateDesc(customerId)
                .stream()
                .map(OrderMapper::entityToDto)
                .toList();
    }

    //GET ORDERS BY RESTAURANT
    @Override
    public List<OrderResponseDto> getOrdersByRestaurant(Long restaurantId) {

        return orderRepository.findByRestaurantIdOrderByOrderDateDesc(restaurantId)
                .stream()
                .map(OrderMapper::entityToDto)
                .toList();
    }

    //GET ACTIVE ORDERS
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

    //UPDATE ORDER STATUS
    @Override
    public OrderResponseDto updateOrderStatus(Long orderId, OrderStatus status) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + orderId));

        order.setOrderStatus(status);

        return OrderMapper.entityToDto(orderRepository.save(order));
    }

    //ASSIGN DELIVERY AGENT
    @Override
    public OrderResponseDto assignDeliveryAgent(Long orderId, Long agentId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + orderId));

        order.setDeliveryAgentId(agentId);

        return OrderMapper.entityToDto(orderRepository.save(order));
    }

    //CANCEL ORDER
    @Override
    public OrderResponseDto cancelOrder(Long orderId, String token) {

        Long customerId = jwtUtil.extractUserId(token);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + orderId));

        if (!order.getCustomerId().equals(customerId)) {
            throw  new UnauthorizedActionException("You are not allowed to perform this action");
        }

        order.setOrderStatus(OrderStatus.CANCELLED);

        return OrderMapper.entityToDto(orderRepository.save(order));
    }

    //REORDER FROM HISTORY
    @Override
    public OrderResponseDto reorderFromHistory(Long orderId, String token) {

        Long customerId = jwtUtil.extractUserId(token);

        Order oldOrder = orderRepository.findById(orderId)
                .orElseThrow(() -> new  OrderNotFoundException("Order not found with id: " + orderId));

        if (!oldOrder.getCustomerId().equals(customerId)) {
            throw new UnauthorizedActionException("You are not allowed to perform this action");
        }

        Order newOrder = new Order();
        newOrder.setCustomerId(customerId);
        newOrder.setRestaurantId(oldOrder.getRestaurantId());
        newOrder.setPaymentMode(oldOrder.getPaymentMode());
        newOrder.setDeliveryAddress(oldOrder.getDeliveryAddress());
        newOrder.setSpecialInstructions(oldOrder.getSpecialInstructions());
        newOrder.setOrderDate(LocalDateTime.now());
        newOrder.setEstimatedDelivery(LocalDateTime.now().plusMinutes(30));

        for (OrderItem item : oldOrder.getItems()) {
            OrderItem newItem = new OrderItem();
            newItem.setMenuItemId(item.getMenuItemId());
            newItem.setName(item.getName());
            newItem.setPrice(item.getPrice());
            newItem.setQuantity(item.getQuantity());
            newItem.setCustomization(item.getCustomization());

            newOrder.addItem(newItem);
        }

        BigDecimal totalAmount = newOrder.getItems().stream()
                .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        newOrder.setTotalAmount(totalAmount);
        newOrder.setDiscount(BigDecimal.ZERO);
        newOrder.setFinalAmount(totalAmount);

        return OrderMapper.entityToDto(orderRepository.save(newOrder));
    }

    //COUNT ORDERS
    @Override
    public Long getOrderCountByRestaurant(Long restaurantId) {
        return orderRepository.countByRestaurantId(restaurantId);
    }
}