package com.quickbite.payment.paymentservice.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handlesKnownExceptionsWithExpectedStatusCodes() {
        assertEquals(HttpStatus.NOT_FOUND,
                handler.handlePaymentNotFound(new PaymentNotFoundException("missing")).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND,
                handler.handleWalletNotFound(new WalletNotFoundException("missing")).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST,
                handler.handleInsufficientBalance(new InsufficientBalanceException("low")).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST,
                handler.handleBadRequest(new InvalidPaymentModeException("invalid")).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST,
                handler.handlePaymentGateway(new PaymentGatewayException("gateway")).getStatusCode());
    }

    @Test
    void handlesGenericExceptions() {
        ResponseEntity<String> response = handler.handleGeneric(new RuntimeException("boom"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Something went wrong", response.getBody());
    }

    @Test
    void collectsValidationErrorsByField() {
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(
                new FieldError("paymentRequestDto", "amount", "must be greater than 0"),
                new FieldError("paymentRequestDto", "mode", "must not be null")
        ));

        ResponseEntity<Map<String, String>> response = handler.handleValidation(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("must be greater than 0", response.getBody().get("amount"));
        assertEquals("must not be null", response.getBody().get("mode"));
    }
}
