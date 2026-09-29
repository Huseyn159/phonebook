package com.floop.phonebook.controller;


import com.floop.phonebook.dto.ImportResponse;
import com.floop.phonebook.dto.PhonebookEntryRequest;
import com.floop.phonebook.dto.PhonebookEntryResponse;
import com.floop.phonebook.dto.SearchRequest;
import com.floop.phonebook.service.PhonebookService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/phonebook")
public class PhonebookController {

    private final PhonebookService phonebookService;


    public PhonebookController(PhonebookService phonebookService) {
        this.phonebookService = phonebookService;

    }

    @PostMapping
    public ResponseEntity<PhonebookEntryResponse> create(@Valid @RequestBody PhonebookEntryRequest request) {
        PhonebookEntryResponse response = phonebookService.create(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PhonebookEntryResponse> update(@PathVariable UUID id,
                                                         @Valid @RequestBody PhonebookEntryRequest request) {
        PhonebookEntryResponse response = phonebookService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PhonebookEntryResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(phonebookService.getById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        phonebookService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/import")
    public ResponseEntity<ImportResponse> importFile(@RequestParam("file") MultipartFile file) {
        ImportResponse response = phonebookService.importFile(file);
        return ResponseEntity.ok(response);
    }


    @PostMapping("/search")
    public Page<PhonebookEntryResponse> search(
            @RequestBody SearchRequest searchRequest,
            Pageable pageable) {
        return phonebookService.search(searchRequest, pageable);
    }

    @PostMapping("/export")
    public ResponseEntity<byte[]> export(@RequestBody SearchRequest searchRequest) throws IOException {
        byte[] excelBytes = phonebookService.export(searchRequest);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=phonebook.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelBytes);
    }



}