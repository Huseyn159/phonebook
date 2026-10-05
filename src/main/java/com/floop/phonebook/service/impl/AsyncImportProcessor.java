package com.floop.phonebook.service.impl;

import com.floop.phonebook.dto.ImportResponse;
import com.floop.phonebook.enums.ResultSetStatus;
import com.floop.phonebook.service.ImportWorker;
import com.floop.phonebook.service.PhonebookService;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

@Component
@Slf4j
public class AsyncImportProcessor {

    private final ImportWorker importWorker;

    public AsyncImportProcessor(ImportWorker importWorker) {
        this.importWorker = importWorker;
    }

    @Async
    public void processImportAsync(UUID resultSetId, String filePath,String email) {
        try (InputStream is = new FileInputStream(filePath)) {
            ImportResponse response = importWorker.processExcelStream(is);
            importWorker.finalizeResultSet(resultSetId, ResultSetStatus.COMPLETED, response, null,email);
        } catch (Exception e) {
            importWorker.finalizeResultSet(resultSetId, ResultSetStatus.FAILED, null, e.getMessage(),email);
        }
        finally {
            deleteFile(filePath,resultSetId);
        }
    }

    private void deleteFile(String filePath,UUID resultSetId) {
       try {
        Files.deleteIfExists(Paths.get(filePath));

       }
       catch (IOException e) {
           log.warn("Could not delete uploaded file for result set {}", resultSetId, e);
       }


    }
}