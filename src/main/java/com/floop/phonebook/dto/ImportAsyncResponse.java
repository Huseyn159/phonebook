package com.floop.phonebook.dto;

import com.floop.phonebook.enums.ResultSetStatus;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;


@Getter
@Setter
public class ImportAsyncResponse {
    private UUID resultSetId;
    private ResultSetStatus status;
}
