package com.floop.phonebook.service.impl;

import com.floop.phonebook.dto.ResultSetResponse;
import com.floop.phonebook.entity.PhonebookResultSet;
import com.floop.phonebook.exception.ResourceNotFoundException;
import com.floop.phonebook.mapper.PhonebookMapper;
import com.floop.phonebook.repository.PhonebookResultSetRepository;
import com.floop.phonebook.service.PhonebookResultSetService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PhonebookResultSetServiceImpl implements PhonebookResultSetService {

    private final PhonebookResultSetRepository phonebookResultSetRepository;
    private final PhonebookMapper mapper;

    public PhonebookResultSetServiceImpl(PhonebookResultSetRepository phonebookResultSetRepository, PhonebookMapper mapper) {
        this.phonebookResultSetRepository = phonebookResultSetRepository;
        this.mapper = mapper;
    }

    @Override
    public ResultSetResponse getById(UUID id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        PhonebookResultSet resultSet = phonebookResultSetRepository.findByIdAndCreatedBy(id, username)
                .orElseThrow(() -> new ResourceNotFoundException("Result set not found: " + id));
        return mapper.toResultSetResponse(resultSet);
    }
}
