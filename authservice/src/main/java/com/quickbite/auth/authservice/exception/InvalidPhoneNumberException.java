package com.quickbite.auth.authservice.exception;

public class InvalidPhoneNumberException extends Exception{

    public InvalidPhoneNumberException() {
        super("Invalid Phone number");
    }
    
    public InvalidPhoneNumberException(String message) {
        super(message);
    }
}
