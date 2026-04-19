package com.quickbite.cart.cartservice.service.serviceImpl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.quickbite.cart.cartservice.client.MenuClient;
import com.quickbite.cart.cartservice.dto.external.MenuItemSnapshotDto;
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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartServiceImpl implements CartService {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private MenuClient menuClient;

    @Override
    public CartResponseDto getCartByCustomerId(Long customerId) {
        Cart cart = cartRepository.findByCustomerId(customerId)
                .orElseGet(() -> cartRepository.save(new Cart(customerId, 0L, BigDecimal.ZERO)));

        List<CartItemResponseDto> items = cartItemRepository.findByCartId(cart.getCartId())
                .stream()
                .map(CartItemMapper::entityToDto)
                .toList();

        return CartMapper.entityToDto(cart, items);
    }

    @Override
    @Transactional
    public CartResponseDto addItemToCart(Long customerId, CartItemRequestDto dto) {
        log.info("Adding menuItemId={} to cart for customerId={}", dto.getMenuItemId(), customerId);

        MenuItemSnapshotDto menuItem = menuClient.getMenuItemById(dto.getMenuItemId());

        if (menuItem == null) {
            throw new RuntimeException("Menu item not found");
        }

        if (!menuItem.isAvailable()) {
            throw new RuntimeException("Menu item is currently unavailable");
        }

        Cart cart = cartRepository.findByCustomerId(customerId)
                .orElseGet(() -> {
                    log.info("Creating new cart for customerId={}", customerId);
                    return cartRepository.save(new Cart(customerId, menuItem.getRestaurantId()));
                });

        if (cart.getRestaurantId() != null
                && cart.getRestaurantId() != 0L
                && !cart.getRestaurantId().equals(menuItem.getRestaurantId())) {
            log.warn("Restaurant mismatch. Clearing cart for customerId={} oldRestaurantId={} newRestaurantId={}",
                    customerId, cart.getRestaurantId(), menuItem.getRestaurantId());

            cartItemRepository.deleteByCartId(cart.getCartId());
            cart.setRestaurantId(menuItem.getRestaurantId());
            cart.setTotalPrice(BigDecimal.ZERO);
            cartRepository.save(cart);
        } else if (cart.getRestaurantId() == null || cart.getRestaurantId() == 0L) {
            cart.setRestaurantId(menuItem.getRestaurantId());
            cartRepository.save(cart);
        }

        List<CartItem> items = cartItemRepository.findByCartId(cart.getCartId());

        Optional<CartItem> existingItem = items.stream()
                .filter(item -> item.getMenuItemId().equals(dto.getMenuItemId()))
                .findFirst();

        BigDecimal effectivePrice = menuItem.getDiscountedPrice() != null
                ? menuItem.getDiscountedPrice()
                : menuItem.getPrice();

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + dto.getQuantity());
            cartItemRepository.save(item);
            log.info("Updated quantity for existing menuItemId={} customerId={}", dto.getMenuItemId(), customerId);
        } else {
            CartItem newItem = new CartItem(
                    cart.getCartId(),
                    menuItem.getItemId(),
                    menuItem.getName(),
                    dto.getQuantity(),
                    effectivePrice,
                    dto.getCustomization()
            );
            cartItemRepository.save(newItem);
            log.info("Added new cart item menuItemId={} customerId={}", dto.getMenuItemId(), customerId);
        }

        recalculateCartTotal(cart);

        List<CartItemResponseDto> responseItems = cartItemRepository.findByCartId(cart.getCartId())
                .stream()
                .map(CartItemMapper::entityToDto)
                .toList();

        return CartMapper.entityToDto(cart, responseItems);
    }

    @Override
    @Transactional
    public void removeItemFromCart(Long customerId, Long itemId) {
        Cart cart = cartRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));

        if (!item.getCartId().equals(cart.getCartId())) {
            throw new RuntimeException("Unauthorized access to cart item");
        }

        cartItemRepository.deleteById(itemId);
        recalculateCartTotal(cart);

        log.info("Removed itemId={} from cart for customerId={}", itemId, customerId);
    }

    @Override
    @Transactional
    public CartItemResponseDto updateCartItemQuantity(Long customerId, Long itemId, Integer quantity) {
        if (quantity == null || quantity < 1) {
            throw new RuntimeException("Quantity must be at least 1");
        }

        Cart cart = cartRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));

        if (!item.getCartId().equals(cart.getCartId())) {
            throw new RuntimeException("Unauthorized access");
        }

        item.setQuantity(quantity);
        CartItem updated = cartItemRepository.save(item);
        recalculateCartTotal(cart);

        log.info("Updated quantity for itemId={} quantity={} customerId={}", itemId, quantity, customerId);
        return CartItemMapper.entityToDto(updated);
    }

    @Override
    @Transactional
    public void clearCart(Long customerId) {
        Cart cart = cartRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        cartItemRepository.deleteByCartId(cart.getCartId());
        cart.setTotalPrice(BigDecimal.ZERO);
        cartRepository.save(cart);

        log.info("Cleared cart for customerId={}", customerId);
    }

    @Override
    @Transactional
    public CartResponseDto changeRestaurant(Long customerId, CartRequestDto dto) {
        Cart cart = cartRepository.findByCustomerId(customerId)
                .orElseGet(() -> cartRepository.save(new Cart(customerId, dto.getRestaurantId())));

        cartItemRepository.deleteByCartId(cart.getCartId());
        cart.setRestaurantId(dto.getRestaurantId());
        cart.setTotalPrice(BigDecimal.ZERO);
        cartRepository.save(cart);

        log.info("Changed cart restaurant for customerId={} restaurantId={}", customerId, dto.getRestaurantId());
        return getCartByCustomerId(customerId);
    }

    @Override
    public CartResponseDto applyPromoCode(Long customerId, String promoCode) {
        log.info("Promo code '{}' requested for customerId={}", promoCode, customerId);
        return getCartByCustomerId(customerId);
    }

    private void recalculateCartTotal(Cart cart) {
        BigDecimal total = cartItemRepository.findByCartId(cart.getCartId())
                .stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        cart.setTotalPrice(total);
        cartRepository.save(cart);
    }
}