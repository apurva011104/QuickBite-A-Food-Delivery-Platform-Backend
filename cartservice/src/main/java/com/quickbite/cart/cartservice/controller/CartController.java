package com.quickbite.cart.cartservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
import com.quickbite.cart.cartservice.security.UserPrincipal;
import com.quickbite.cart.cartservice.service.CartService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping("/me")
    public ResponseEntity<CartResponseDto> getCart(Authentication authentication) {
        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(cartService.getCartByCustomerId(user.getUserId()));
    }

    @PostMapping("/add")
    public ResponseEntity<CartResponseDto> addItem(@Valid @RequestBody CartItemRequestDto dto,
                                                   Authentication authentication) {
        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(cartService.addItemToCart(user.getUserId(), dto));
    }

    @DeleteMapping("/remove/{itemId}")
    public ResponseEntity<String> removeItem(@PathVariable Long itemId,
                                             Authentication authentication) {
        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
        cartService.removeItemFromCart(user.getUserId(), itemId);
        return ResponseEntity.ok("Item removed successfully");
    }

    @PutMapping("/update/{itemId}")
    public ResponseEntity<CartItemResponseDto> updateQuantity(@PathVariable Long itemId,
                                                              @RequestParam Integer quantity,
                                                              Authentication authentication) {
        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(cartService.updateCartItemQuantity(user.getUserId(), itemId, quantity));
    }

    @DeleteMapping("/clear")
    public ResponseEntity<String> clearCart(Authentication authentication) {
        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
        cartService.clearCart(user.getUserId());
        return ResponseEntity.ok("Cart cleared successfully");
    }

    @PutMapping("/change-restaurant")
    public ResponseEntity<CartResponseDto> changeRestaurant(@Valid @RequestBody CartRequestDto dto,
                                                            Authentication authentication) {
        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(cartService.changeRestaurant(user.getUserId(), dto));
    }

    @PostMapping("/apply-promo")
    public ResponseEntity<CartResponseDto> applyPromo(@RequestParam String promoCode,
                                                      Authentication authentication) {
        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(cartService.applyPromoCode(user.getUserId(), promoCode));
    }
}