package com.quickbite.cart.cartservice.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import com.quickbite.cart.cartservice.dto.requestDto.CartItemRequestDto;
import com.quickbite.cart.cartservice.dto.requestDto.CartRequestDto;
import com.quickbite.cart.cartservice.dto.responseDto.CartItemResponseDto;
import com.quickbite.cart.cartservice.dto.responseDto.CartResponseDto;
import com.quickbite.cart.cartservice.security.UserPrincipal;
import com.quickbite.cart.cartservice.service.CartService;

@ExtendWith(MockitoExtension.class)
class CartControllerTest {

    @Mock
    private CartService cartService;

    private CartController cartController;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        cartController = new CartController(cartService);
        UserPrincipal principal = new UserPrincipal(1L, "customer@quickbite.com", "CUSTOMER");
        authentication = new UsernamePasswordAuthenticationToken(principal, null, List.of(() -> "ROLE_CUSTOMER"));
    }

    @Test
    void getCartShouldReturnCurrentUserCart() {
        when(cartService.getCartByCustomerId(1L)).thenReturn(cartResponse());

        ResponseEntity<CartResponseDto> response = cartController.getCart(authentication);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody().getCartId()).isEqualTo(100L);
    }

    @Test
    void addItemShouldDelegateToService() {
        CartItemRequestDto request = new CartItemRequestDto(20L, 2, "No onion");
        when(cartService.addItemToCart(1L, request)).thenReturn(cartResponse());

        ResponseEntity<CartResponseDto> response = cartController.addItem(request, authentication);

        assertThat(response.getBody().getTotalPrice()).isEqualByComparingTo("498");
    }

    @Test
    void removeItemShouldReturnSuccessMessage() {
        ResponseEntity<String> response = cartController.removeItem(200L, authentication);

        verify(cartService).removeItemFromCart(1L, 200L);
        assertThat(response.getBody()).isEqualTo("Item removed successfully");
    }

    @Test
    void updateQuantityShouldReturnUpdatedItem() {
        when(cartService.updateCartItemQuantity(1L, 200L, 5)).thenReturn(
                new CartItemResponseDto(200L, 100L, 20L, "Paneer Wrap", 5, BigDecimal.valueOf(249), "No onion"));

        ResponseEntity<CartItemResponseDto> response = cartController.updateQuantity(200L, 5, authentication);

        assertThat(response.getBody().getQuantity()).isEqualTo(5);
    }

    @Test
    void clearCartShouldReturnSuccessMessage() {
        ResponseEntity<String> response = cartController.clearCart(authentication);

        verify(cartService).clearCart(1L);
        assertThat(response.getBody()).isEqualTo("Cart cleared successfully");
    }

    @Test
    void changeRestaurantShouldReturnUpdatedCart() {
        when(cartService.changeRestaurant(1L, new CartRequestDto(77L))).thenReturn(cartResponse());

        ResponseEntity<CartResponseDto> response = cartController.changeRestaurant(new CartRequestDto(77L), authentication);

        assertThat(response.getBody().getRestaurantId()).isEqualTo(10L);
    }

    @Test
    void applyPromoShouldDelegateToService() {
        when(cartService.applyPromoCode(1L, "SAVE10")).thenReturn(cartResponse());

        ResponseEntity<CartResponseDto> response = cartController.applyPromo("SAVE10", authentication);

        assertThat(response.getBody().getCartItems()).hasSize(1);
        verify(cartService).applyPromoCode(1L, "SAVE10");
    }

    private CartResponseDto cartResponse() {
        return new CartResponseDto(100L, 1L, 10L, BigDecimal.valueOf(498),
                List.of(new CartItemResponseDto(200L, 100L, 20L, "Paneer Wrap", 2, BigDecimal.valueOf(249), "No onion")));
    }
}
