package com.floop.phonebook.controller;


import com.floop.phonebook.dto.EsSearchRequest;
import com.floop.phonebook.dto.EsSearchResponse;
import com.floop.phonebook.service.EsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/phonebook/es")
public class EsController {

    private final EsService esService;


    public EsController(EsService esService) {
        this.esService = esService;
    }


    @PostMapping("/search")
    public ResponseEntity<EsSearchResponse> search(
            @RequestBody EsSearchRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
            ){

        return ResponseEntity.ok(esService.esProcess(request,page,size));
    }
}
