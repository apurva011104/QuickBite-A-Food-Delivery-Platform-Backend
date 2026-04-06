package com.quickbite.auth.authservice.exception;

public class InvalidPasswordException extends Exception{
    
    public InvalidPasswordException(){
        super("Invalid Password!");
    }

    public InvalidPasswordException(String message){
        super(message);
    }
}
