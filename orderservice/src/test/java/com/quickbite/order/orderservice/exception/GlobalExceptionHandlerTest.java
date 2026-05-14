package com.quickbite.order.orderservice.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest();
        request.setRequestURI("/orders/100");
    }

    @Test
    void shouldHandleOrderNotFound() {
        var response = handler.handleOrderNotFound(new OrderNotFoundException("missing"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody().getMessage()).isEqualTo("missing");
    }

    @Test
    void shouldHandleUnauthorizedAction() {
        var response = handler.handleUnauthorized(new UnauthorizedActionException("forbidden"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(403);
        assertThat(response.getBody().getError()).isEqualTo("Forbidden");
    }

    @Test
    void shouldHandleInvalidOrderState() {
        var response = handler.handleInvalidState(new InvalidOrderStateException("bad state"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().getError()).isEqualTo("Bad Request");
    }

    @Test
    void shouldHandlePaymentModeError() {
        var response = handler.handlePayment(new PaymentModeNotAvailableException("payment unavailable"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().getMessage()).isEqualTo("payment unavailable");
    }

    @Test
    void shouldHandleEmptyOrder() {
        var response = handler.handleEmptyOrder(new EmptyOrderException("empty"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().getMessage()).isEqualTo("empty");
    }

    @Test
    void shouldHandleGenericException() {
        var response = handler.handleGeneric(new RuntimeException("boom"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody().getMessage()).isEqualTo("Something went wrong");
    }
}
