package com.floop.phonebook.controller;

import com.floop.phonebook.entity.PhonebookResultSet;
import com.floop.phonebook.service.PhoneBookResultSetService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/result-set")
public class ResultSetController {

    private final PhoneBookResultSetService resultSetService;

    public ResultSetController(PhoneBookResultSetService resultSetService) {
        this.resultSetService = resultSetService;
    }


    @GetMapping("/{id}")
    public ResponseEntity<PhonebookResultSet> getResultSet(@PathVariable UUID id) {

        return ResponseEntity.ok(resultSetService.getById(id));
    }
}
