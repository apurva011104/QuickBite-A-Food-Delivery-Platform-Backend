package com.quickbite.auth.authservice.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.quickbite.auth.authservice.exception.InvalidEmailException;
import com.quickbite.auth.authservice.exception.InvalidPasswordException;
import com.quickbite.auth.authservice.exception.InvalidPhoneNumberException;

class ValidatorUtilityTest {

    @Test
    void validateEmailShouldAcceptWellFormedEmail() {
        assertDoesNotThrow(() -> ValidatorUtility.validateEmail("alice@example.com"));
    }

    @Test
    void validateEmailShouldRejectMalformedEmail() {
        assertThrows(InvalidEmailException.class, () -> ValidatorUtility.validateEmail("alice.example.com"));
    }

    @Test
    void validatePhoneNumberShouldAllowBlankValues() {
        assertDoesNotThrow(() -> ValidatorUtility.validatePhoneNumber(" "));
    }

    @Test
    void validatePhoneNumberShouldRejectInvalidIndianMobileNumber() {
        assertThrows(InvalidPhoneNumberException.class, () -> ValidatorUtility.validatePhoneNumber("12345"));
    }

    @Test
    void validatePasswordShouldAcceptStrongPassword() {
        assertDoesNotThrow(() -> ValidatorUtility.validatePassword("Password1"));
    }

    @Test
    void validatePasswordShouldRejectWeakPassword() {
        assertThrows(InvalidPasswordException.class, () -> ValidatorUtility.validatePassword("password"));
    }
}
