package com.floop.phonebook.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ImportError {
    private int row;
    private String message;

    public ImportError(int row, String message) {
        this.row = row;
        this.message = message;
    }
}