package com.floop.phonebook.search;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.floop.phonebook.dto.SearchCriteria;
import com.floop.phonebook.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

class EsQueryBuilderTest {

    private EsQueryBuilder esQueryBuilder;

    @BeforeEach
    void setUp() {
        esQueryBuilder = new EsQueryBuilder();
    }

    private SearchCriteria criteria(String key, SearchOperation op, Object value) {
        SearchCriteria c = new SearchCriteria();
        c.setKey(key);
        c.setOperation(op);
        c.setValue(value);
        return c;
    }


    @ParameterizedTest
    @NullAndEmptySource
    void build_whenNoCriteria_returnsMatchAll(List<SearchCriteria> criteriaList){
        Query query = esQueryBuilder.build(criteriaList);

        assertThat(query.isMatchAll()).isTrue();
    }

    @Test
    void build_whenTextFieldUsedInEqual_returnsRaw(){
        SearchCriteria criteria = criteria("name",SearchOperation.EQUAL,"Ali");

        Query query = esQueryBuilder.build(List.of(criteria));

        Query term = query.bool().filter().get(0);
        assertThat(term.isTerm()).isTrue();
        assertThat(term.term().field()).isEqualTo("name.raw");
        assertThat(term.term().value().stringValue()).isEqualTo("Ali");
    }

    @Test
    void build_whenKeywordFieldInEqual_returnsSameField(){
        SearchCriteria criteria = criteria("city",SearchOperation.EQUAL,"Baku");

        Query query = esQueryBuilder.build(List.of(criteria));

        Query term = query.bool().filter().get(0);
        assertThat(term.term().field()).isEqualTo("city");
    }

    @Test
    void build_whenInvalidField_throwsBadRequest(){
        SearchCriteria criteria = criteria("password",SearchOperation.EQUAL,"123456");

       assertThatThrownBy(()->esQueryBuilder.build(List.of(criteria)))
               .isInstanceOf(BadRequestException.class)
               .hasMessageContaining("Invalid search field: "+criteria.getKey());
    }

    @Test
    void build_whenCriteriaItemIsNull_throwsBadRequest(){
        SearchCriteria criteria = null;
        assertThatThrownBy(()->esQueryBuilder.build(Arrays.asList(criteria)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Criteria item must not be null");
    }

    @Test
    void build_whenOperationFieldIsNull_throwsBadRequest(){
        SearchCriteria criteria = criteria("city",null,"Baku");

        assertThatThrownBy(()->esQueryBuilder.build(List.of(criteria)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Search operation is required");
    }

    @Test
    void build_whenValueIsNull_throwsBadRequest(){
        SearchCriteria criteria = criteria("city",SearchOperation.EQUAL,null);

        assertThatThrownBy(()->esQueryBuilder.build(List.of(criteria)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Value is required for " + criteria.getOperation());

    }

    @Test
    void build_whenUnsupportedSearchOperationForEs_throwsBadRequest(){
        SearchCriteria criteria = criteria("createdAt",SearchOperation.LAST_DAY,"2026-11-11");

        assertThatThrownBy(()->esQueryBuilder.build(List.of(criteria)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Unsupported operation type: " + criteria.getOperation());
    }

    @Test
    void build_whenMatchOperationOnKeywordField_throwsBadRequest(){
        SearchCriteria criteria = criteria("city",SearchOperation.MATCH,"Baku");

        assertThatThrownBy(()->esQueryBuilder.build(List.of(criteria)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("MATCH is only supported for text field:name,surname,address");
    }

    @Test
    void build_whenRangeOperationOnTextField_throwsBadRequest(){
        SearchCriteria criteria = criteria("name",SearchOperation.GREATER_THAN,"Baku");

        assertThatThrownBy(()->esQueryBuilder.build(List.of(criteria)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Range operators are only supported for date fields");
    }

    @Test
    void build_whenUseRangeOperationWithInvalidValue_throwsBadRequest(){
        SearchCriteria criteria = criteria("activatedDate",SearchOperation.GREATER_THAN,"abc");

        assertThatThrownBy(()->esQueryBuilder.build(List.of(criteria)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid date for " + criteria.getKey() + ": " + criteria.getValue());
    }

    @Test
    void build_whenUseRangeOperationWithValidValues_thenReturnsSameValue(){
        SearchCriteria criteria = criteria("activatedDate",SearchOperation.GREATER_THAN,"2026-01-01");
        Query query = esQueryBuilder.build(List.of(criteria));

        Query range = query.bool().filter().get(0);
        assertThat(range.range().date().gt()).isEqualTo(criteria.getValue());
    }

    @Test
    void build_whenUseBetweenOperationWithSingleValuedList_throwsBadRequestException(){
        SearchCriteria criteria = criteria("activatedDate",SearchOperation.BETWEEN,List.of("2026-01-01"));

        assertThatThrownBy(()->esQueryBuilder.build(List.of(criteria)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Please enter 2 value");
    }





}