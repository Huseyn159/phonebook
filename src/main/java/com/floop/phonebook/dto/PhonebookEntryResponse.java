package com.floop.phonebook.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class PhonebookEntryResponse {

    private UUID id;
    private String name;
    private String surname;
    private String nationalId;
    private LocalDate dateOfBirth;
    private String fin;
    private String address;
    private String city;
    private String number;
    private LocalDate activatedDate;
    private LocalDate stopDate;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}