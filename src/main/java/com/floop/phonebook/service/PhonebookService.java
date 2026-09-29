package com.floop.phonebook.service;


import com.floop.phonebook.dto.PhonebookEntryRequest;
import com.floop.phonebook.dto.PhonebookEntryResponse;
import com.floop.phonebook.dto.SearchRequest;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public interface PhonebookService {

    PhonebookEntryResponse create(PhonebookEntryRequest request);

    PhonebookEntryResponse getById(UUID id);

    PhonebookEntryResponse update(UUID id, PhonebookEntryRequest request);

    Page<PhonebookEntryResponse> search(SearchRequest searchRequest, Pageable pageable);

    byte[] export(SearchRequest searchRequest) throws IOException;



    void delete(UUID id);
}