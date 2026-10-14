package com.floop.phonebook.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.floop.phonebook.dto.PhonebookEntryResponse;
import com.floop.phonebook.dto.SearchCriteria;
import com.floop.phonebook.entity.PhonebookEntry;
import com.floop.phonebook.entity.PhonebookResultSet;
import com.floop.phonebook.enums.ResultSetStatus;
import com.floop.phonebook.enums.ResultSetType;
import com.floop.phonebook.mapper.PhonebookMapper;
import com.floop.phonebook.repository.PhonebookRepository;
import com.floop.phonebook.repository.PhonebookResultSetRepository;
import com.floop.phonebook.search.PhonebookSpecificationBuilder;
import com.floop.phonebook.service.EsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class EsServiceImpl implements EsService {

    private final PhonebookMapper mapper;
    private final ElasticsearchClient client;
    private final PhonebookRepository phonebookRepository;
    private final PhonebookSpecificationBuilder specificationBuilder;
    private final int maxSnapshots;
    private final PhonebookResultSetRepository resultSetRepository;
    private final ObjectMapper objectMapper;
    private final int retentionDays;

    public EsServiceImpl(PhonebookMapper mapper, ElasticsearchClient client, PhonebookRepository phonebookRepository,
                         PhonebookSpecificationBuilder specificationBuilder,
                         @Value("${phonebook.es.max-snapshot-rows}") int maxSnapshots,
                         PhonebookResultSetRepository resultSetRepository, ObjectMapper objectMapper,
                         @Value("${phonebook.result-set.retention-days}") int retentionDays) {
        this.mapper = mapper;
        this.client = client;
        this.phonebookRepository = phonebookRepository;
        this.specificationBuilder = specificationBuilder;
        this.maxSnapshots = maxSnapshots;
        this.resultSetRepository = resultSetRepository;
        this.objectMapper = objectMapper;
        this.retentionDays = retentionDays;
    }

    private PhonebookResultSet createSnapshot(List<SearchCriteria> criteria){
        PhonebookResultSet resultSet = createInProgressResultSet(criteria);
        String indexName = buildIndexName(resultSet.getId());

        try {
            Page<PhonebookEntry> page = fetchMatchingRows(criteria);
            createIndex(indexName);
            bulkIndex(indexName, page.getContent());

            resultSet.setIndexName(indexName);
            resultSet.setStatus(ResultSetStatus.COMPLETED);
            resultSet.setTotalRows(page.getContent().size());
            resultSet.setFinishedAt(LocalDateTime.now());
            resultSetRepository.save(resultSet);
            return resultSet;
        }catch (RuntimeException e){
            resultSet.setStatus(ResultSetStatus.FAILED);
            resultSet.setErrorSummary(e.getMessage());
            resultSet.setFinishedAt(LocalDateTime.now());
            resultSetRepository.save(resultSet);
            throw e;
        }
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

    private String buildIndexName(UUID resultSetId){
        return "phonebook-search-" + resultSetId;
    }

    private void createIndex(String indexName){
        try {
            client.indices().create(i -> i
                    .index(indexName)
                    .settings(c -> c.numberOfShards("1").numberOfReplicas("0"))
                    .mappings(p -> p
                            .properties("id", t -> t.keyword(k -> k))
                            .properties("name", t -> t.text(n -> n.fields("raw", v -> v.keyword(k -> k))))
                            .properties("surname", t -> t.text(n -> n.fields("raw", v -> v.keyword(k -> k))))
                            .properties("nationalId", t -> t.keyword(k -> k))
                            .properties("dateOfBirth", t -> t.date(d -> d))
                            .properties("fin", t -> t.keyword(k -> k))
                            .properties("address", t -> t.text(f -> f.fields("raw", v -> v.keyword(k -> k))))
                            .properties("city", t -> t.keyword(k -> k))
                            .properties("number", t -> t.keyword(k -> k))
                            .properties("activatedDate", t -> t.date(d -> d))
                            .properties("stopDate", t -> t.date(d -> d))
                            .properties("active", t -> t.boolean_(b -> b))
                            .properties("createdAt", t -> t.date(d -> d))
                            .properties("updatedAt", t -> t.date(d -> d))
                    ));
        }catch (IOException e){
            throw new IllegalStateException("Could not create index " + indexName, e);
        }

    }

    private void bulkIndex(String indexName,List<PhonebookEntry> rows){
        if (rows.isEmpty()){
            return;
        }
        BulkRequest.Builder br = new BulkRequest.Builder();
        for (PhonebookEntry row:rows){
            PhonebookEntryResponse doc = mapper.toResponse(row);
            br.operations(op->op
                    .index(i->i
                            .index(indexName)
                            .id(row.getId().toString())
                            .document(doc)));
        }
        br.refresh(Refresh.WaitFor);

        try{
            BulkResponse response = client.bulk(br.build());
            if (response.errors()){
                for (BulkResponseItem item : response.items()){
                    if (item.error()!=null){
                        throw new IllegalStateException(  "Bulk indexing failed for document " + item.id() + ": " + item.error().reason());
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Bulk indexing failed for " + indexName,e);
        }
    }


}
