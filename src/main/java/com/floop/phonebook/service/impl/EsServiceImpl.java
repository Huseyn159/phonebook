package com.floop.phonebook.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.floop.phonebook.dto.EsSearchRequest;
import com.floop.phonebook.dto.EsSearchResponse;
import com.floop.phonebook.dto.PhonebookEntryResponse;
import com.floop.phonebook.dto.SearchCriteria;
import com.floop.phonebook.entity.PhonebookEntry;
import com.floop.phonebook.entity.PhonebookResultSet;
import com.floop.phonebook.enums.ResultSetStatus;
import com.floop.phonebook.enums.ResultSetType;
import com.floop.phonebook.exception.BadRequestException;
import com.floop.phonebook.exception.ConflictException;
import com.floop.phonebook.exception.ResourceNotFoundException;
import com.floop.phonebook.mapper.PhonebookMapper;
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
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
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

    private static final int MAX_RESULT_WINDOW = 10_000;

    @Override
    public EsSearchResponse esProcess(EsSearchRequest request, int page, int size) {

        if (page<0||size<1){
            throw new BadRequestException("page must be >= 0 and size must be >= 1");
        }
        if ((long) page * size + size > MAX_RESULT_WINDOW) {
            throw new BadRequestException("page * size + size must not exceed " + MAX_RESULT_WINDOW);
        }

        PhonebookResultSet resultSet;
        if (request.getResultSetId()==null){
            resultSet = createSnapshot(request.getCriteria());
        }else {
            resultSet = loadUsableResultSet(request.getResultSetId(),request.getCriteria());
        }

        return readPage(resultSet,page,size);


    }


    private EsSearchResponse readPage(PhonebookResultSet resultSet, int page, int size) {
        SearchResponse<PhonebookEntryResponse> response;
        try{
            response =client.search(s->s
                    .index(resultSet.getIndexName())
                    .from(page*size)
                    .size(size)
                    .sort(so->so.field(f->f.field("createdAt").order(SortOrder.Desc)))
                    .sort(so->so.field(f->f.field("id").order(SortOrder.Asc))), PhonebookEntryResponse.class);
        }
        catch (ElasticsearchException e) {
            if (e.status() == 404) {
                throw new ResourceNotFoundException(resultSet.getId());
            }
            throw e;
        }
        catch (IOException e){
            throw new IllegalStateException("Could not search index" + resultSet.getIndexName(),e);
        }

        List<PhonebookEntryResponse> content = new ArrayList<>();

        for (Hit<PhonebookEntryResponse> hit : response.hits().hits()){
            content.add(hit.source());
        }

        long total = response.hits().total().value();
        EsSearchResponse result = new EsSearchResponse();
        result.setResultSetId(resultSet.getId());
        result.setContent(content);
        result.setPage(page);
        result.setSize(size);
        result.setTotalElements(total);
        result.setTotalPages((int) Math.ceil((double) total/size));
        return result;
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
        }
        catch (RuntimeException e){
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


    private PhonebookResultSet loadUsableResultSet(UUID id,List<SearchCriteria> criteria){
        PhonebookResultSet resultSet = resultSetRepository.findById(id)
                .orElseThrow(() ->new ResourceNotFoundException(id));

        if (resultSet.getType()!=ResultSetType.SEARCH){
            throw new BadRequestException("Result set " + id + " is not a search result set");
        }

        if (resultSet.getExpirationTime() != null
                 && resultSet.getExpirationTime().isBefore(LocalDateTime.now())){
            throw new ResourceNotFoundException(id);
        }

        if (resultSet.getStatus()!=ResultSetStatus.COMPLETED){
            throw new ConflictException("Result set " + id + " is not ready, status: " + resultSet.getStatus());
        }

        //MUVEQQETI YOXLAMA !!!
        if(criteria != null && !criteria.isEmpty()){
            throw new BadRequestException("Extra criteria inside a snapshot are not supported yet");
        }

        return resultSet;

    }


    @Scheduled(fixedDelayString = "${phonebook.result-set.cleanup-interval-ms:3600000}")
    public void deleteExpired(){
        List<PhonebookResultSet> resultSets = resultSetRepository.findByTypeEqualsAndExpirationTimeIsBefore(ResultSetType.SEARCH,LocalDateTime.now());
        for (PhonebookResultSet resultSet : resultSets){
            if (deleteIndex(buildIndexName(resultSet.getId()))){
                resultSetRepository.delete(resultSet);
            }
        }
    }


    private boolean deleteIndex(String indexName){
        try{
            client.indices().delete(i->i.index(indexName));
            return true;
        }
        catch(ElasticsearchException e){
            if (e.status() == 404){
                return true;
            }
            log.warn("Could not delete index {}", indexName, e);
            return false;
        } catch (IOException e) {
            log.warn("Could not delete index {}", indexName, e);
            return false;
        }

    }


}
