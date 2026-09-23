package com.floop.phonebook.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class PhonebookEntryRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    @NotBlank
    @Size(max = 100)
    private String surname;

    @Size(max = 50)
    private String nationalId;

    private LocalDate dateOfBirth;

    @Size(max = 20)
    private String fin;

    @Size(max = 255)
    private String address;

    @Size(max = 100)
    private String city;

    @Size(max = 30)
    private String number;

    @NotNull
    private LocalDate activatedDate;

    private LocalDate stopDate;
}