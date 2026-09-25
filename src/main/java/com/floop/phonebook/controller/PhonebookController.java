package com.floop.phonebook.controller;


import com.floop.phonebook.dto.ImportResponse;
import com.floop.phonebook.dto.PhonebookEntryRequest;
import com.floop.phonebook.dto.PhonebookEntryResponse;
import com.floop.phonebook.dto.SearchRequest;
import com.floop.phonebook.service.ImportService;
import com.floop.phonebook.service.PhonebookService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/phonebook")
public class PhonebookController {

    private final PhonebookService service;
    private final ImportService importService;

    public PhonebookController(PhonebookService service, ImportService importService) {
        this.service = service;
        this.importService = importService;
    }

    @PostMapping
    public ResponseEntity<PhonebookEntryResponse> create(@Valid @RequestBody PhonebookEntryRequest request) {
        PhonebookEntryResponse response = service.create(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PhonebookEntryResponse> update(@PathVariable UUID id,
                                                         @Valid @RequestBody PhonebookEntryRequest request) {
        PhonebookEntryResponse response = service.update(id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PhonebookEntryResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/import")
    public ResponseEntity<ImportResponse> importFile(@RequestParam("file") MultipartFile file) {
        ImportResponse response = importService.importFile(file);
        return ResponseEntity.ok(response);
    }


    @PostMapping("/search")
    public Page<PhonebookEntryResponse> search(
            @RequestBody SearchRequest searchRequest,
            Pageable pageable) {
        return service.search(searchRequest, pageable);
    }



}