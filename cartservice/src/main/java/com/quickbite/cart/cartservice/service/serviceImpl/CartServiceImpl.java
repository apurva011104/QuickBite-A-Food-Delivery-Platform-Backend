package com.quickbite.cart.cartservice.service.serviceImpl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.quickbite.cart.cartservice.dto.requestDto.CartItemRequestDto;
import com.quickbite.cart.cartservice.dto.requestDto.CartRequestDto;
import com.quickbite.cart.cartservice.dto.responseDto.CartItemResponseDto;
import com.quickbite.cart.cartservice.dto.responseDto.CartResponseDto;
import com.quickbite.cart.cartservice.entity.Cart;
import com.quickbite.cart.cartservice.entity.CartItem;
import com.quickbite.cart.cartservice.mapper.CartItemMapper;
import com.quickbite.cart.cartservice.mapper.CartMapper;
import com.quickbite.cart.cartservice.repository.CartItemRepository;
import com.quickbite.cart.cartservice.repository.CartRepository;
import com.quickbite.cart.cartservice.service.CartService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CartServiceImpl implements CartService {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    //GET CART
    @Override
    public CartResponseDto getCartByCustomerId(Long customerId) {

        Cart cart = cartRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        List<CartItemResponseDto> items = cartItemRepository.findByCartId(cart.getCartId())
                .stream()
                .map(CartItemMapper::entityToDto)
                .collect(Collectors.toList());

        return CartMapper.entityToDto(cart, items);
    }

    //ADD ITEM
    @Override
    public CartResponseDto addItemToCart(CartItemRequestDto dto) {

        log.info("Adding item {} to cart for customer {}", dto.getMenuItemId(), dto.getCustomerId());

        Cart cart = cartRepository.findByCustomerId(dto.getCustomerId())
                .orElseGet(() -> {
                    log.info("Creating new cart for customer {}", dto.getCustomerId());
                    return cartRepository.save(
                            new Cart(dto.getCustomerId(), dto.getRestaurantId())
                    );
                });

        if (!cart.getRestaurantId().equals(dto.getRestaurantId())) {
            log.warn("Restaurant mismatch. Clearing cart for customer {}", dto.getCustomerId());
            cartItemRepository.deleteByCartId(cart.getCartId());
            cart.setRestaurantId(dto.getRestaurantId());
            cart.setTotalPrice(BigDecimal.ZERO);
        }

        List<CartItem> items = cartItemRepository.findByCartId(cart.getCartId());

        Optional<CartItem> existingItem = items.stream()
                .filter(item -> item.getMenuItemId().equals(dto.getMenuItemId()))
                .findFirst();

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + dto.getQuantity());
            cartItemRepository.save(item);

            log.info("Updated quantity for item {}", item.getMenuItemId());

        } else {
            CartItem newItem = CartItemMapper.dtoToEntity(dto, cart.getCartId());
            cartItemRepository.save(newItem);

            log.info("Added new item {}", dto.getMenuItemId());
        }

        BigDecimal total = cartItemRepository.findByCartId(cart.getCartId())
                                            .stream()
                                            .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                                            .reduce(BigDecimal.ZERO, BigDecimal::add);

        cart.setTotalPrice(total);
        cartRepository.save(cart);

        List<CartItemResponseDto> responseItems = cartItemRepository.findByCartId(cart.getCartId())
                .stream()
                .map(CartItemMapper::entityToDto)
                .collect(Collectors.toList());

        return CartMapper.entityToDto(cart, responseItems);
    }

    //REMOVE ITEM
    @Override
    public void removeItemFromCart(Long customerId, Long itemId) {

        Cart cart = cartRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));

        if (!item.getCartId().equals(cart.getCartId())) {
            throw new RuntimeException("Unauthorized access to cart item");
        }

        cartItemRepository.deleteById(itemId);

        log.info("Removed item {} from cart", itemId);
    }

    //UPDATE QUANTITY
    @Override
    public CartItemResponseDto updateCartItemQuantity(Long customerId, Long itemId, Integer quantity) {

        Cart cart = cartRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));

        if (!item.getCartId().equals(cart.getCartId())) {
            throw new RuntimeException("Unauthorized access");
        }

        item.setQuantity(quantity);
        cartItemRepository.save(item);

        log.info("Updated quantity for item {}", itemId);

        return CartItemMapper.entityToDto(item);
    }

    //CLEAR CART
    @Override
    public void clearCart(Long customerId) {

        Cart cart = cartRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        cartItemRepository.deleteByCartId(cart.getCartId());

        cart.setTotalPrice(BigDecimal.ZERO);
        cartRepository.save(cart);

        log.info("Cleared cart for customer {}", customerId);
    }

    //CHANGE RESTAURANT
    @Override
    public CartResponseDto changeRestaurant(CartRequestDto dto) {

        clearCart(dto.getCustomerId());

        Cart cart = cartRepository.findByCustomerId(dto.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        cart.setRestaurantId(dto.getRestaurantId());
        cartRepository.save(cart);

        return getCartByCustomerId(dto.getCustomerId());
    }

    //PROMO (STUB)
    @Override
    public CartResponseDto applyPromoCode(Long customerId, String promoCode) {

        log.info("Applying promo code {} for customer {}", promoCode, customerId);
        return getCartByCustomerId(customerId);
    }
}