package com.quickbite.cart.cartservice.service.serviceImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.quickbite.cart.cartservice.client.MenuClient;
import com.quickbite.cart.cartservice.dto.external.MenuItemSnapshotDto;
import com.quickbite.cart.cartservice.dto.requestDto.CartItemRequestDto;
import com.quickbite.cart.cartservice.dto.requestDto.CartRequestDto;
import com.quickbite.cart.cartservice.dto.responseDto.CartItemResponseDto;
import com.quickbite.cart.cartservice.dto.responseDto.CartResponseDto;
import com.quickbite.cart.cartservice.entity.Cart;
import com.quickbite.cart.cartservice.entity.CartItem;
import com.quickbite.cart.cartservice.repository.CartItemRepository;
import com.quickbite.cart.cartservice.repository.CartRepository;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private MenuClient menuClient;

    @InjectMocks
    private CartServiceImpl cartService;

    private Cart cart;
    private CartItem cartItem;
    private CartItemRequestDto cartItemRequestDto;
    private MenuItemSnapshotDto menuItemSnapshotDto;

    @BeforeEach
    void setUp() {
        cart = new Cart(1L, 10L, BigDecimal.valueOf(498));
        cart.setCartId(100L);

        cartItem = new CartItem(100L, 20L, "Paneer Wrap", 2, BigDecimal.valueOf(249), "No onion");
        cartItem.setItemId(200L);

        cartItemRequestDto = new CartItemRequestDto(20L, 2, "No onion");

        menuItemSnapshotDto = new MenuItemSnapshotDto();
        menuItemSnapshotDto.setItemId(20L);
        menuItemSnapshotDto.setRestaurantId(10L);
        menuItemSnapshotDto.setName("Paneer Wrap");
        menuItemSnapshotDto.setPrice(BigDecimal.valueOf(299));
        menuItemSnapshotDto.setDiscountedPrice(BigDecimal.valueOf(249));
        menuItemSnapshotDto.setAvailable(true);
    }

    @Test
    void getCartByCustomerIdShouldReturnExistingCart() {
        when(cartRepository.findByCustomerId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(100L)).thenReturn(List.of(cartItem));

        CartResponseDto result = cartService.getCartByCustomerId(1L);

        assertThat(result.getCartId()).isEqualTo(100L);
        assertThat(result.getCartItems()).hasSize(1);
        assertThat(result.getTotalPrice()).isEqualByComparingTo("498");
    }

    @Test
    void getCartByCustomerIdShouldCreateCartWhenMissing() {
        when(cartRepository.findByCustomerId(5L)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> {
            Cart saved = invocation.getArgument(0);
            saved.setCartId(500L);
            return saved;
        });
        when(cartItemRepository.findByCartId(500L)).thenReturn(List.of());

        CartResponseDto result = cartService.getCartByCustomerId(5L);

        assertThat(result.getCartId()).isEqualTo(500L);
        assertThat(result.getRestaurantId()).isZero();
        assertThat(result.getCartItems()).isEmpty();
    }

    @Test
    void addItemToCartShouldThrowWhenMenuItemMissing() {
        when(menuClient.getMenuItemById(20L)).thenReturn(null);

        assertThatThrownBy(() -> cartService.addItemToCart(1L, cartItemRequestDto))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Menu item not found");
    }

    @Test
    void addItemToCartShouldThrowWhenMenuItemUnavailable() {
        menuItemSnapshotDto.setAvailable(false);
        when(menuClient.getMenuItemById(20L)).thenReturn(menuItemSnapshotDto);

        assertThatThrownBy(() -> cartService.addItemToCart(1L, cartItemRequestDto))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Menu item is currently unavailable");
    }

    @Test
    void addItemToCartShouldCreateNewCartAndAddItem() {
        when(menuClient.getMenuItemById(20L)).thenReturn(menuItemSnapshotDto);
        when(cartRepository.findByCustomerId(1L)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> {
            Cart saved = invocation.getArgument(0);
            if (saved.getCartId() == null) {
                saved.setCartId(100L);
            }
            return saved;
        });
        when(cartItemRepository.findByCartId(100L)).thenReturn(
                List.of(),
                List.of(new CartItem(100L, 20L, "Paneer Wrap", 2, BigDecimal.valueOf(249), "No onion")),
                List.of(new CartItem(100L, 20L, "Paneer Wrap", 2, BigDecimal.valueOf(249), "No onion")));

        CartResponseDto result = cartService.addItemToCart(1L, cartItemRequestDto);

        assertThat(result.getRestaurantId()).isEqualTo(10L);
        assertThat(result.getTotalPrice()).isEqualByComparingTo("498");
        assertThat(result.getCartItems()).hasSize(1);
        verify(cartItemRepository).save(any(CartItem.class));
    }

    @Test
    void addItemToCartShouldIncreaseQuantityForExistingItem() {
        when(menuClient.getMenuItemById(20L)).thenReturn(menuItemSnapshotDto);
        when(cartRepository.findByCustomerId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(100L)).thenReturn(
                List.of(cartItem),
                List.of(cartItem),
                List.of(cartItem));
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartResponseDto result = cartService.addItemToCart(1L, new CartItemRequestDto(20L, 1, "No onion"));

        assertThat(cartItem.getQuantity()).isEqualTo(3);
        assertThat(result.getTotalPrice()).isEqualByComparingTo("747");
    }

    @Test
    void addItemToCartShouldClearCartWhenRestaurantChanges() {
        menuItemSnapshotDto.setRestaurantId(99L);
        when(menuClient.getMenuItemById(20L)).thenReturn(menuItemSnapshotDto);
        when(cartRepository.findByCustomerId(1L)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(cartItemRepository.findByCartId(100L)).thenReturn(
                List.of(),
                List.of(new CartItem(100L, 20L, "Paneer Wrap", 2, BigDecimal.valueOf(249), "No onion")),
                List.of(new CartItem(100L, 20L, "Paneer Wrap", 2, BigDecimal.valueOf(249), "No onion")));
        doNothing().when(cartItemRepository).deleteByCartId(100L);

        CartResponseDto result = cartService.addItemToCart(1L, cartItemRequestDto);

        verify(cartItemRepository).deleteByCartId(100L);
        assertThat(cart.getRestaurantId()).isEqualTo(99L);
        assertThat(result.getRestaurantId()).isEqualTo(99L);
    }

    @Test
    void addItemToCartShouldUseActualPriceWhenDiscountMissing() {
        menuItemSnapshotDto.setDiscountedPrice(null);
        when(menuClient.getMenuItemById(20L)).thenReturn(menuItemSnapshotDto);
        when(cartRepository.findByCustomerId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(100L)).thenReturn(
                List.of(),
                List.of(new CartItem(100L, 20L, "Paneer Wrap", 2, BigDecimal.valueOf(299), "No onion")),
                List.of(new CartItem(100L, 20L, "Paneer Wrap", 2, BigDecimal.valueOf(299), "No onion")));
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartResponseDto result = cartService.addItemToCart(1L, cartItemRequestDto);

        assertThat(result.getTotalPrice()).isEqualByComparingTo("598");
    }

    @Test
    void removeItemFromCartShouldDeleteWhenOwnedByCustomer() {
        when(cartRepository.findByCustomerId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findById(200L)).thenReturn(Optional.of(cartItem));
        when(cartItemRepository.findByCartId(100L)).thenReturn(List.of());

        cartService.removeItemFromCart(1L, 200L);

        verify(cartItemRepository).deleteById(200L);
        assertThat(cart.getTotalPrice()).isEqualByComparingTo("0");
    }

    @Test
    void removeItemFromCartShouldRejectUnauthorizedItem() {
        CartItem foreignItem = new CartItem(999L, 20L, "Paneer Wrap", 1, BigDecimal.valueOf(249), null);
        foreignItem.setItemId(200L);
        when(cartRepository.findByCustomerId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findById(200L)).thenReturn(Optional.of(foreignItem));

        assertThatThrownBy(() -> cartService.removeItemFromCart(1L, 200L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Unauthorized access to cart item");
    }

    @Test
    void updateCartItemQuantityShouldRejectInvalidQuantity() {
        assertThatThrownBy(() -> cartService.updateCartItemQuantity(1L, 200L, 0))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Quantity must be at least 1");
    }

    @Test
    void updateCartItemQuantityShouldUpdateAndReturnDto() {
        when(cartRepository.findByCustomerId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findById(200L)).thenReturn(Optional.of(cartItem));
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(cartItemRepository.findByCartId(100L)).thenReturn(List.of(cartItem));

        CartItemResponseDto result = cartService.updateCartItemQuantity(1L, 200L, 5);

        assertThat(result.getQuantity()).isEqualTo(5);
        assertThat(cart.getTotalPrice()).isEqualByComparingTo("1245");
    }

    @Test
    void updateCartItemQuantityShouldRejectUnauthorizedAccess() {
        CartItem foreignItem = new CartItem(999L, 20L, "Paneer Wrap", 1, BigDecimal.valueOf(249), null);
        foreignItem.setItemId(200L);
        when(cartRepository.findByCustomerId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findById(200L)).thenReturn(Optional.of(foreignItem));

        assertThatThrownBy(() -> cartService.updateCartItemQuantity(1L, 200L, 2))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Unauthorized access");
    }

    @Test
    void clearCartShouldResetTotal() {
        when(cartRepository.findByCustomerId(1L)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        cartService.clearCart(1L);

        verify(cartItemRepository).deleteByCartId(100L);
        assertThat(cart.getTotalPrice()).isEqualByComparingTo("0");
    }

    @Test
    void clearCartShouldThrowWhenCartMissing() {
        when(cartRepository.findByCustomerId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.clearCart(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Cart not found");

        verify(cartItemRepository, never()).deleteByCartId(any());
    }

    @Test
    void changeRestaurantShouldResetExistingCart() {
        when(cartRepository.findByCustomerId(1L)).thenReturn(Optional.of(cart), Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(cartItemRepository.findByCartId(100L)).thenReturn(List.of());

        CartResponseDto result = cartService.changeRestaurant(1L, new CartRequestDto(77L));

        verify(cartItemRepository).deleteByCartId(100L);
        assertThat(result.getRestaurantId()).isEqualTo(77L);
        assertThat(result.getTotalPrice()).isEqualByComparingTo("0");
    }

    @Test
    void changeRestaurantShouldCreateCartWhenMissing() {
        Cart createdCart = new Cart(8L, 77L, BigDecimal.ZERO);
        createdCart.setCartId(801L);
        when(cartRepository.findByCustomerId(8L)).thenReturn(Optional.empty(), Optional.of(createdCart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> {
            Cart saved = invocation.getArgument(0);
            if (saved.getCartId() == null) {
                saved.setCartId(801L);
            }
            return saved;
        });
        when(cartItemRepository.findByCartId(801L)).thenReturn(List.of());

        CartResponseDto result = cartService.changeRestaurant(8L, new CartRequestDto(77L));

        assertThat(result.getRestaurantId()).isEqualTo(77L);
    }

    @Test
    void applyPromoCodeShouldReturnCurrentCart() {
        when(cartRepository.findByCustomerId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(100L)).thenReturn(List.of(cartItem));

        CartResponseDto result = cartService.applyPromoCode(1L, "SAVE10");

        assertThat(result.getCartId()).isEqualTo(100L);
        assertThat(result.getCartItems()).hasSize(1);
    }
}
