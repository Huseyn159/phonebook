package com.floop.phonebook.dto;


import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class EsSearchRequest {

    private UUID resultSetId;
    private List<SearchCriteria> criteria;

}
