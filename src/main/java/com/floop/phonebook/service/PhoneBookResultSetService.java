package com.floop.phonebook.service;

import com.floop.phonebook.entity.PhonebookResultSet;

import java.util.UUID;

public interface PhoneBookResultSetService {
    PhonebookResultSet getById(UUID id);
}
