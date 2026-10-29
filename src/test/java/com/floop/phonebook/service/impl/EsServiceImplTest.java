package com.floop.phonebook.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.floop.phonebook.dto.EsSearchRequest;
import com.floop.phonebook.entity.PhonebookResultSet;
import com.floop.phonebook.enums.ResultSetStatus;
import com.floop.phonebook.enums.ResultSetType;
import com.floop.phonebook.exception.BadRequestException;
import com.floop.phonebook.exception.ConflictException;
import com.floop.phonebook.exception.ResourceNotFoundException;
import com.floop.phonebook.mapper.PhonebookMapper;
import com.floop.phonebook.repository.PhonebookRepository;
import com.floop.phonebook.repository.PhonebookResultSetRepository;
import com.floop.phonebook.search.EsQueryBuilder;
import com.floop.phonebook.search.PhonebookSpecificationBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EsServiceImplTest {


    @Mock private PhonebookMapper mapper;
    @Mock private ElasticsearchClient client;
    @Mock private PhonebookRepository phonebookRepository;
    @Mock private PhonebookSpecificationBuilder specificationBuilder;
    @Mock private PhonebookResultSetRepository resultSetRepository;
    @Mock private ObjectMapper objectMapper;
    @Mock private EsQueryBuilder esQueryBuilder;

    private EsServiceImpl esService;

    @BeforeEach
    void setUp(){
        esService = new EsServiceImpl(mapper,client,phonebookRepository,specificationBuilder,10_000,
                resultSetRepository,objectMapper,30,esQueryBuilder);
    }

    private EsSearchRequest requestFor(UUID id){
        EsSearchRequest request = new EsSearchRequest();
        request.setResultSetId(id);
        return request;
    }

    private PhonebookResultSet usableResultSet(){
        PhonebookResultSet rs = new PhonebookResultSet();
        rs.setType(ResultSetType.SEARCH);
        rs.setStatus(ResultSetStatus.COMPLETED);
        rs.setExpirationTime(LocalDateTime.now().plusDays(1));
        return rs;
    }


    @Test
    void esProcess_whenResultSetIsNotSearch_throwsBadRequest(){
        UUID id = UUID.randomUUID();
        PhonebookResultSet rs = usableResultSet();
        rs.setType(ResultSetType.IMPORT);
        when(resultSetRepository.findById(id)).thenReturn(Optional.of(rs));


        assertThatThrownBy(()->esService.esProcess(requestFor(id),0,10))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not a search result set");
    }

    @Test
    void esProcess_whenResultSetNotFound_throwsResourceNotFound(){
        UUID id = UUID.randomUUID();

        assertThatThrownBy(()->esService.esProcess(requestFor(id),0,10))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Result set not found: " + id);

    }

    @Test
    void esProcess_whenResultSetIsExpired_throwsResourceNotFound(){
        UUID id = UUID.randomUUID();
        PhonebookResultSet rs = usableResultSet();
        rs.setExpirationTime(LocalDateTime.now().minusDays(1));

        when(resultSetRepository.findById(id)).thenReturn(Optional.of(rs));

        assertThatThrownBy(()->esService.esProcess(requestFor(id),0,10))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Result set not found: " + id);

    }

    @Test
    void esProcess_whenResultSetStatusInProgress_throwsConflictException(){
        UUID id = UUID.randomUUID();
        PhonebookResultSet rs = usableResultSet();
        rs.setStatus(ResultSetStatus.IN_PROGRESS);
        when(resultSetRepository.findById(id)).thenReturn(Optional.of(rs));

        assertThatThrownBy(()->esService.esProcess(requestFor(id),0,10))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("not ready");
    }

    @Test
    void esProcess_whenInvalidPageValue_throwsBadRequest(){
        UUID id = UUID.randomUUID();

        assertThatThrownBy(()->esService.esProcess(requestFor(id),-1,10))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("page must be >= 0 and size must be >= 1");

        verifyNoInteractions(resultSetRepository);

    }




}