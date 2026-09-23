package com.floop.phonebook.service;

import com.floop.phonebook.dto.ImportResponse;
import org.springframework.web.multipart.MultipartFile;



public interface ImportService {
    ImportResponse importFile(MultipartFile file);
}
