package com.quickbite.auth.authservice.exception;

public class InvalidEmailException extends Exception {

    public InvalidEmailException() {
        super("Invalid email");
    }

    public InvalidEmailException(String message) {
        super(message);
    }

}
