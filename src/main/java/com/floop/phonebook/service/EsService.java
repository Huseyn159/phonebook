package com.floop.phonebook.service;

import com.floop.phonebook.dto.SearchCriteria;
import com.floop.phonebook.entity.PhonebookEntry;
import org.springframework.data.domain.Page;

import java.util.List;

public interface EsService {

    Page<PhonebookEntry> fetchMatchingRows(List<SearchCriteria> criteria);
}
