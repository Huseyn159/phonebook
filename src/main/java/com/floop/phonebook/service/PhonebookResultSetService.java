package com.floop.phonebook.service;

import com.floop.phonebook.dto.ResultSetResponse;
import com.floop.phonebook.entity.PhonebookResultSet;

import java.util.UUID;

public interface PhonebookResultSetService {
    ResultSetResponse getById(UUID id);
}
