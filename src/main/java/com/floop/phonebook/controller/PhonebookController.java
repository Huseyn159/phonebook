package com.floop.phonebook.controller;


import com.floop.phonebook.dto.*;
import com.floop.phonebook.entity.PhonebookResultSet;
import com.floop.phonebook.service.ImportProcessingService;
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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/phonebook")
public class PhonebookController {

    private final PhonebookService phonebookService;
    private final ImportProcessingService importProcessingService;


    public PhonebookController(PhonebookService phonebookService, ImportProcessingService importProcessingService) {
        this.phonebookService = phonebookService;

        this.importProcessingService = importProcessingService;
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
        ImportResponse response = importProcessingService.importFile(file);
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

    @PostMapping("/fields")
    public Page<String> distinctValues(
            @RequestParam String field,
            @RequestParam(required = false) String searchValue,
            @RequestBody(required = false) SearchRequest searchRequest,
            Pageable pageable) {
        return phonebookService.distinctValues(field, searchValue, searchRequest, pageable);
    }


    @PostMapping("/import/async")
    public ResponseEntity<ImportAsyncResponse> importAsync(@RequestParam("file") MultipartFile file) throws IOException {
        PhonebookResultSet resultSet = importProcessingService.importAsync(file);  // phonebookService YOX, importProcessingService

        ImportAsyncResponse response = new ImportAsyncResponse();
        response.setResultSetId(resultSet.getId());
        response.setStatus(resultSet.getStatus());
        return ResponseEntity.ok(response);
    }



}