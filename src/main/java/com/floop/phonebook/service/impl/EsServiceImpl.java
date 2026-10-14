package com.floop.phonebook.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.floop.phonebook.dto.SearchCriteria;
import com.floop.phonebook.entity.PhonebookEntry;
import com.floop.phonebook.entity.PhonebookResultSet;
import com.floop.phonebook.enums.ResultSetStatus;
import com.floop.phonebook.enums.ResultSetType;
import com.floop.phonebook.exception.BadRequestException;
import com.floop.phonebook.repository.PhonebookRepository;
import com.floop.phonebook.repository.PhonebookResultSetRepository;
import com.floop.phonebook.search.PhonebookSpecificationBuilder;
import com.floop.phonebook.service.EsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class EsServiceImpl implements EsService {

    private final PhonebookRepository phonebookRepository;
    private final PhonebookSpecificationBuilder specificationBuilder;
    private final int maxSnapshots;
    private final PhonebookResultSetRepository resultSetRepository;
    private final ObjectMapper objectMapper;
    private final int retentionDays;

    public EsServiceImpl(PhonebookRepository phonebookRepository,
                         PhonebookSpecificationBuilder specificationBuilder,
                         @Value("${phonebook.es.max-snapshot-rows}") int maxSnapshots,
                         PhonebookResultSetRepository resultSetRepository, ObjectMapper objectMapper,
                         @Value("${phonebook.result-set.retention-days}") int retentionDays) {
        this.phonebookRepository = phonebookRepository;
        this.specificationBuilder = specificationBuilder;
        this.maxSnapshots = maxSnapshots;
        this.resultSetRepository = resultSetRepository;
        this.objectMapper = objectMapper;
        this.retentionDays = retentionDays;
    }

    private Page<PhonebookEntry> fetchMatchingRows(List<SearchCriteria> criteria){
        Pageable cap = PageRequest.of(0,maxSnapshots);
        Specification<PhonebookEntry> spec = specificationBuilder.buildAll(criteria);
        return phonebookRepository.findAll(spec,cap);

    }

    private PhonebookResultSet createInProgressResultSet(List<SearchCriteria> criteria){
        PhonebookResultSet resultSet = new PhonebookResultSet();
        resultSet.setType(ResultSetType.SEARCH);
        resultSet.setStatus(ResultSetStatus.IN_PROGRESS);
        try {
            resultSet.setCriteriaJson(objectMapper.writeValueAsString(criteria));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize search criteria", e);
        }
        LocalDateTime now = LocalDateTime.now();
        resultSet.setCreatedAt(now);
        resultSet.setExpirationTime(now.plusDays(retentionDays));
        return resultSetRepository.save(resultSet);
    }



}
