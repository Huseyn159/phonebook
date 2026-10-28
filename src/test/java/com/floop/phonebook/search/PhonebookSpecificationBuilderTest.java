package com.floop.phonebook.search;

import com.floop.phonebook.dto.SearchCriteria;
import com.floop.phonebook.entity.PhonebookEntry;
import com.floop.phonebook.exception.BadRequestException;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class PhonebookSpecificationBuilderTest {


    private PhonebookSpecificationBuilder specificationBuilder;

    @Mock private Root<PhonebookEntry> root;
    @Mock private CriteriaQuery<?> query;
    @Mock private CriteriaBuilder cb;
    @Mock private Path<Object> path;

    private void fieldType(String field,Class<?> type){
        doReturn(path).when(root).get(field);
        doReturn(type).when(path).getJavaType();
    }

    @BeforeEach
    void setUp() {
        specificationBuilder = new PhonebookSpecificationBuilder();
    }

    private SearchCriteria criteria(String key, SearchOperation op, Object value) {
        SearchCriteria c = new SearchCriteria();
        c.setKey(key);
        c.setOperation(op);
        c.setValue(value);
        return c;
    }


    @Test
    void build_whenCriteriaNull_throwsBadRequest(){
        assertThatThrownBy(()->specificationBuilder.build(null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("must not be null");
    }

    @Test
    void build_whenKeyIsBlank_throwsBadRequest(){
        SearchCriteria criteria = criteria(" ",SearchOperation.EQUAL,"Baku");

        assertThatThrownBy(()->specificationBuilder.build(criteria))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("key is required");
    }

    @Test
    void build_whenOperationIsNull_throwsBadRequest(){
        SearchCriteria criteria = criteria("name",null,"Baku");

        assertThatThrownBy(()->specificationBuilder.build(criteria))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Search operation is required");
    }

    @Test
    void build_whenEqualWithInvalidField_throwsBadRequest(){
        SearchCriteria criteria = criteria("password",SearchOperation.EQUAL,"1234567");

        assertThatThrownBy(()->specificationBuilder.build(criteria))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid search field");
    }

    @Test
    void build_whenEqualWithNullValue_throwsBadRequest() {
        SearchCriteria criteria = criteria("name", SearchOperation.EQUAL, null);

        assertThatThrownBy(()->specificationBuilder.build(criteria))
                .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("Value is required");
    }
    @Test
    void build_whenLastDayWithNullValue_doesNotRequireValue(){
        SearchCriteria criteria = criteria("activatedDate",SearchOperation.LAST_DAY,null);

        assertThat(specificationBuilder.build(criteria)).isNotNull();
    }

    @Test
    void buildAll_whenUsedWithNullList_returnsAll(){
        List<SearchCriteria> criteriaList = null;

        assertThat(specificationBuilder.buildAll(criteriaList)).isNotNull();
    }

    @Test
    void toPredicate_whenBetweenWithOneValueList_throwsBadRequest(){
        fieldType("activatedDate", LocalDate.class);
        SearchCriteria criteria = criteria("activatedDate",SearchOperation.BETWEEN,List.of("2026-01-01"));

        Specification<PhonebookEntry> spec
                = specificationBuilder.build(criteria);


        assertThatThrownBy(()->spec.toPredicate(root,query,cb)).isInstanceOf(BadRequestException.class).hasMessageContaining("exactly 2 values");
    }

    @Test
    void toPredicate_whenRangeOperatorWithInvalidValue_throwsBadRequest(){
        fieldType("activatedDate",LocalDate.class);
        SearchCriteria criteria = criteria("activatedDate",SearchOperation.GREATER_THAN,"abc");

        Specification<PhonebookEntry> spec =
                specificationBuilder.build(criteria);

        assertThatThrownBy(()->spec.toPredicate(root,query,cb))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid value");
    }

    @Test
    void toPredicate_whenMatchWithInvalidField_throwsBadRequest(){
        fieldType("activatedDate",LocalDate.class);
        SearchCriteria criteria = criteria("activatedDate",SearchOperation.MATCH,"abc");
        Specification<PhonebookEntry> spec =
                specificationBuilder.build(criteria);

        assertThatThrownBy(()->spec.toPredicate(root,query,cb))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("only supported for text fields");
    }

    @Test
    void toPredicate_whenMatchWithIntegerValue_throwsBadRequest(){
        fieldType("name", String.class);
        SearchCriteria criteria = criteria("name",SearchOperation.MATCH,123);
        Specification<PhonebookEntry> spec =
                specificationBuilder.build(criteria);

        assertThatThrownBy(()->spec.toPredicate(root,query,cb))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("require a text value");
    }

    @Test
    void toPredicate_whenLastDayWithNullStringField_throwsBadRequest(){
        fieldType("name",String.class);
        SearchCriteria criteria = criteria("name",SearchOperation.LAST_DAY,null);
        Specification<PhonebookEntry> spec =
                specificationBuilder.build(criteria);

        assertThatThrownBy(()->spec.toPredicate(root,query,cb))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("only supported for date fields");
    }

    @Test
    void toPredicate_whenLastHourWithLocalDateField_throwsBadRequest(){
        fieldType("activatedDate", LocalDate.class);
        SearchCriteria criteria = criteria("activatedDate",SearchOperation.LAST_HOUR,"2026-01-01");
        Specification<PhonebookEntry> spec =
                specificationBuilder.build(criteria);

        assertThatThrownBy(()->spec.toPredicate(root,query,cb))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("LAST_HOUR and LAST_MINUTE are only supported for LocalDateTime");
    }

    @Test
    void toPredicate_whenLastHourOnDateTimeField_doesNotThrowException(){
        fieldType("createdAt",LocalDateTime.class);
        SearchCriteria criteria = criteria("createdAt",SearchOperation.LAST_HOUR,null);
        Specification<PhonebookEntry> spec =
                 specificationBuilder.build(criteria);

        assertThatNoException().isThrownBy(()->spec.toPredicate(root,query,cb));
    }

}