package com.floop.phonebook.service;


import com.floop.phonebook.dto.PhonebookEntryRequest;
import com.floop.phonebook.dto.PhonebookEntryResponse;

import java.util.UUID;

public interface PhonebookService {

    PhonebookEntryResponse create(PhonebookEntryRequest request);

    PhonebookEntryResponse getById(UUID id);

    PhonebookEntryResponse update(UUID id, PhonebookEntryRequest request);

    void delete(UUID id);
}