package com.floop.phonebook.repository;

import com.floop.phonebook.entity.PhonebookResultSet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PhonebookResultSetRepository extends JpaRepository<PhonebookResultSet, UUID> {

}
