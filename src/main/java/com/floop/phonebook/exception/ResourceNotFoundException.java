package com.floop.phonebook.exception;

import java.util.UUID;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(UUID id) {
        super("Phonebook entry not found: " + id);
    }
}
