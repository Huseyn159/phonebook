package com.floop.phonebook.repository;

import com.floop.phonebook.entity.PhonebookResultSet;
import com.floop.phonebook.enums.ResultSetType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface PhonebookResultSetRepository extends JpaRepository<PhonebookResultSet, UUID> {

    List<PhonebookResultSet> findByTypeEqualsAndExpirationTimeIsBefore(ResultSetType type,LocalDateTime time);
}
