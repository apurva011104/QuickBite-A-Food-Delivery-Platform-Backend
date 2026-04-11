package com.quickbite.menu.menuservice.exception;

public class RestaurantNotFoundException extends Exception {

    public RestaurantNotFoundException() {
        super("No such restaurant found");
    }
    
    public RestaurantNotFoundException(String message) {
        super(message);
    }
}
