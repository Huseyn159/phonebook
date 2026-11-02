package com.floop.phonebook.controller;


import com.floop.phonebook.dto.EsSearchRequest;
import com.floop.phonebook.dto.EsSearchResponse;
import com.floop.phonebook.service.EsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Snapshot search")
@RestController
@RequestMapping("/api/v1/phonebook/es")
public class EsController {

    private final EsService esService;

    public EsController(EsService esService) {
        this.esService = esService;
    }

    @Operation(summary = "Search via an Elasticsearch snapshot",
            description = "Without resultSetId, creates a new snapshot from PostgreSQL. "
                    + "With resultSetId, narrows the existing snapshot without creating a new one.")
    @PostMapping("/search")
    public ResponseEntity<EsSearchResponse> search(@RequestBody EsSearchRequest request,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(esService.esProcess(request, page, size));
    }
}