package com.floop.phonebook.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class EsSearchResponse {
    private UUID resultSetId;
    private List<PhonebookEntryResponse> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean truncated;
}
