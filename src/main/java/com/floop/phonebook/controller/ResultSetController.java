package com.floop.phonebook.controller;

import com.floop.phonebook.dto.ResultSetResponse;
import com.floop.phonebook.entity.PhonebookResultSet;
import com.floop.phonebook.service.PhonebookResultSetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Result sets")
@RestController
@RequestMapping("/api/v1/result-set")
public class ResultSetController {

    private final PhonebookResultSetService resultSetService;

    public ResultSetController(PhonebookResultSetService resultSetService) {
        this.resultSetService = resultSetService;
    }

    @Operation(summary = "Get import or snapshot status")
    @GetMapping("/{id}")
    public ResponseEntity<ResultSetResponse> getResultSet(@PathVariable UUID id) {
        return ResponseEntity.ok(resultSetService.getById(id));
    }
}