package com.floop.phonebook.service;

import com.floop.phonebook.dto.ImportResponse;
import com.floop.phonebook.entity.PhonebookResultSet;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;


public interface ImportProcessingService {
    ImportResponse importFile(MultipartFile file);
    PhonebookResultSet importAsync(MultipartFile file) throws IOException;
}
