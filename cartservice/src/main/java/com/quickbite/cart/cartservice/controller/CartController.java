package com.quickbite.cart.cartservice.controller;


import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.quickbite.cart.cartservice.dto.requestDto.CartItemRequestDto;
import com.quickbite.cart.cartservice.dto.requestDto.CartRequestDto;
import com.quickbite.cart.cartservice.dto.responseDto.CartItemResponseDto;
import com.quickbite.cart.cartservice.dto.responseDto.CartResponseDto;
import com.quickbite.cart.cartservice.service.CartService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    // ---------------- GET CART ----------------
    @GetMapping("/{customerId}")
    public CartResponseDto getCart(@PathVariable Long customerId) {
        return cartService.getCartByCustomerId(customerId);
    }

    // ---------------- ADD ITEM ----------------
    @PostMapping("/add")
    public CartResponseDto addItem(@RequestBody CartItemRequestDto dto) {
        return cartService.addItemToCart(dto);
    }

    // ---------------- REMOVE ITEM ----------------
    @DeleteMapping("/remove/{customerId}/{itemId}")
    public void removeItem(@PathVariable Long customerId,
                           @PathVariable Long itemId) {
        cartService.removeItemFromCart(customerId, itemId);
    }

    // ---------------- UPDATE QUANTITY ----------------
    @PutMapping("/update/{customerId}/{itemId}")
    public CartItemResponseDto updateQuantity(@PathVariable Long customerId,
                                              @PathVariable Long itemId,
                                              @RequestParam Integer quantity) {
        return cartService.updateCartItemQuantity(customerId, itemId, quantity);
    }

    // ---------------- CLEAR CART ----------------
    @DeleteMapping("/clear/{customerId}")
    public void clearCart(@PathVariable Long customerId) {
        cartService.clearCart(customerId);
    }

    // ---------------- CHANGE RESTAURANT ----------------
    @PutMapping("/change-restaurant")
    public CartResponseDto changeRestaurant(@RequestBody CartRequestDto dto) {
        return cartService.changeRestaurant(dto);
    }

    // ---------------- APPLY PROMO ----------------
    @PostMapping("/apply-promo")
    public CartResponseDto applyPromo(@RequestParam Long customerId,
                                     @RequestParam String promoCode) {
        return cartService.applyPromoCode(customerId, promoCode);
    }
}