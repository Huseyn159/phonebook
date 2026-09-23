package com.floop.phonebook.repository;

import com.floop.phonebook.entity.PhonebookEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PhonebookRepository extends JpaRepository<PhonebookEntry, UUID> {

    List<PhonebookEntry> findByNumberAndActiveTrue(String number);

    List<PhonebookEntry> findByNumberAndStopDateIsNotNullOrderByStopDateDesc(String number);

    Optional<PhonebookEntry> findByNumberAndActivatedDate(String number, LocalDate activatedDate);
}
