package com.floop.phonebook.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SearchRequest {

    private List<SearchCriteria> criteria;
}
