package com.floop.phonebook.service.impl;

import com.floop.phonebook.dto.SearchCriteria;
import com.floop.phonebook.entity.PhonebookEntry;
import com.floop.phonebook.repository.PhonebookRepository;
import com.floop.phonebook.search.PhonebookSpecificationBuilder;
import com.floop.phonebook.service.EsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;

import java.util.List;

public class EsServiceImpl implements EsService {

    private final PhonebookRepository phonebookRepository;
    private final PhonebookSpecificationBuilder specificationBuilder;
    private final int maxSnapshots;

    public EsServiceImpl(PhonebookRepository phonebookRepository,
                         PhonebookSpecificationBuilder specificationBuilder,
                         @Value("${phonebook.es.max-snapshot-rows}") int maxSnapshots) {
        this.phonebookRepository = phonebookRepository;
        this.specificationBuilder = specificationBuilder;
        this.maxSnapshots = maxSnapshots;
    }


    @Override
    public Page<PhonebookEntry> fetchMatchingRows(List<SearchCriteria> criteria) {
        return null;
    }
}
