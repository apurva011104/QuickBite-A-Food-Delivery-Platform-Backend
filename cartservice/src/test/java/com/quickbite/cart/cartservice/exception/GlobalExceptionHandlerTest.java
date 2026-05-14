package com.quickbite.cart.cartservice.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.HandlerMethod;
import org.springframework.security.access.AccessDeniedException;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest();
        request.setRequestURI("/cart/test");
    }

    @Test
    void shouldHandleRestaurantNotFound() throws Exception {
        var response = handler.handleRestaurantNotFound(new RestaurantNotFoundException("Restaurant missing"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody().getError()).isEqualTo("Restaurant Not Found");
    }

    @Test
    void shouldHandleCustomerNotFound() throws Exception {
        var response = handler.handleCustomerNotFound(new CustomerNotFoundException("Customer missing"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody().getError()).isEqualTo("Customer Not Found");
    }

    @Test
    void shouldHandleResourceNotFound() {
        var response = handler.handleResourceNotFound(new ResourceNotFoundException("Item missing"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody().getError()).isEqualTo("Not Found");
    }

    @Test
    void shouldHandleBadRequest() {
        var response = handler.handleBadRequest(new BadRequestException("Invalid input"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().getError()).isEqualTo("Bad Request");
    }

    @Test
    void shouldHandleValidationErrors() throws Exception {
        CartPayload target = new CartPayload();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(target, "cartPayload");
        bindingResult.addError(new FieldError("cartPayload", "quantity", "must be greater than or equal to 1"));
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(
                new HandlerMethod(this, getClass().getDeclaredMethod("helper", CartPayload.class)).getMethodParameters()[0],
                bindingResult);

        var response = handler.handleValidation(exception);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isEqualTo(Map.of("quantity", "must be greater than or equal to 1"));
    }

    @Test
    void shouldHandleAccessDenied() {
        var response = handler.handleAccessDenied(new AccessDeniedException("Denied"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(403);
        assertThat(response.getBody().getError()).isEqualTo("Forbidden");
    }

    @Test
    void shouldHandleGenericException() {
        var response = handler.handleGenericException(new RuntimeException("Boom"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody().getMessage()).isEqualTo("Boom");
    }

    void helper(CartPayload payload) {
    }

    private static final class CartPayload {
        private Integer quantity;
    }
}
