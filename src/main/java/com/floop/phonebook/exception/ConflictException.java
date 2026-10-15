package com.floop.phonebook.exception;

public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
