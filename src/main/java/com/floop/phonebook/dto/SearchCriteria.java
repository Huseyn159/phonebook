package com.floop.phonebook.dto;


import com.floop.phonebook.search.SearchOperation;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SearchCriteria {

    private String key;
    private Object value;
    private SearchOperation operation;


}
