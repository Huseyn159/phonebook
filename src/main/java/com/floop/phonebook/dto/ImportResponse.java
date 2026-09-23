package com.floop.phonebook.dto;


import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ImportResponse {

    private int totalRows;
    private int created;
    private int updated;
    private int failed;
    private List<ImportError> errors = new ArrayList<>();


}
