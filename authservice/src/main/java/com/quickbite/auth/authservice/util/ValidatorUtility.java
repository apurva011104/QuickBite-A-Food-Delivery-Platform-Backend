package com.quickbite.auth.authservice.util;

import com.quickbite.auth.authservice.exception.InvalidEmailException;
import com.quickbite.auth.authservice.exception.InvalidPasswordException;
import com.quickbite.auth.authservice.exception.InvalidPhoneNumberException;

public class ValidatorUtility {

    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
    private static final String PHONE_NUMBER_REGEX = "^[6-9]\\d{9}$";
    private static final String PASSWORD_REGEX = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$";

    private ValidatorUtility() {
    }

    public static void validateEmail(String email) throws InvalidEmailException {
        if (email == null || email.isBlank() || !email.matches(EMAIL_REGEX)) {
            throw new InvalidEmailException("Invalid email " + email);
        }
    }

    public static void validatePhoneNumber(String phoneNumber) throws InvalidPhoneNumberException {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return; 
        }

        if (!phoneNumber.matches(PHONE_NUMBER_REGEX)) {
            throw new InvalidPhoneNumberException("Invalid phone number " + phoneNumber);
        }
    }

    public static void validatePassword(String password) throws InvalidPasswordException {
        if (password == null || password.isBlank() || !password.matches(PASSWORD_REGEX)) {
            throw new InvalidPasswordException(
                    "Password must contain at least one uppercase letter, one lowercase letter, one digit and must be at least eight characters long."
            );
        }
    }
}