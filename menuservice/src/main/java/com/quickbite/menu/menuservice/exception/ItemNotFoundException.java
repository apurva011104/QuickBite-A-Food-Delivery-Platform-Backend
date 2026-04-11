package com.quickbite.menu.menuservice.exception;

public class ItemNotFoundException extends Exception {

    public ItemNotFoundException() {
        super("No such item found");
    }

    public ItemNotFoundException(String message) {
        super(message);
    }
    
}
