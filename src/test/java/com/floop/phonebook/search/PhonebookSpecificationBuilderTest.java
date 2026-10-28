package com.floop.phonebook.search;

import com.floop.phonebook.dto.SearchCriteria;
import com.floop.phonebook.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

class PhonebookSpecificationBuilderTest {


    private PhonebookSpecificationBuilder specificationBuilder;


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

}