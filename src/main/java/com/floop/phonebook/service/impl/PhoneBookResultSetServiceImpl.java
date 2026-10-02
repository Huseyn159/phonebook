package com.floop.phonebook.service.impl;

import com.floop.phonebook.entity.PhonebookResultSet;
import com.floop.phonebook.exception.BadRequestException;
import com.floop.phonebook.exception.ResourceNotFoundException;
import com.floop.phonebook.repository.PhonebookResultSetRepository;
import com.floop.phonebook.service.PhoneBookResultSetService;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PhoneBookResultSetServiceImpl implements PhoneBookResultSetService {

    private final PhonebookResultSetRepository phonebookResultSetRepository;

    public PhoneBookResultSetServiceImpl(PhonebookResultSetRepository phonebookResultSetRepository) {
        this.phonebookResultSetRepository = phonebookResultSetRepository;
    }

    @Override
    public PhonebookResultSet getById(UUID id) {
        PhonebookResultSet resultSet = phonebookResultSetRepository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException(id));
        return resultSet;
    }
}
