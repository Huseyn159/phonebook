package com.floop.phonebook.service;

import com.floop.phonebook.dto.EsSearchRequest;
import com.floop.phonebook.dto.EsSearchResponse;

public interface EsService {

EsSearchResponse esProcess(EsSearchRequest request,int page,int size);

}
