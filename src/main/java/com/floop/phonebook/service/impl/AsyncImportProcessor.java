package com.floop.phonebook.service.impl;

import com.floop.phonebook.dto.ImportResponse;
import com.floop.phonebook.enums.ResultSetStatus;
import com.floop.phonebook.service.ImportWorker;
import com.floop.phonebook.service.PhonebookService;
import jakarta.validation.Validator;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.UUID;

@Component
public class AsyncImportProcessor {

    private final ImportWorker importWorker;

    public AsyncImportProcessor(ImportWorker importWorker) {
        this.importWorker = importWorker;
    }

    @Async
    public void processImportAsync(UUID resultSetId, String filePath) {
        try (InputStream is = new FileInputStream(filePath)) {
            ImportResponse response = importWorker.processExcelStream(is);
            importWorker.finalizeResultSet(resultSetId, ResultSetStatus.COMPLETED, response, null);
        } catch (Exception e) {
            importWorker.finalizeResultSet(resultSetId, ResultSetStatus.FAILED, null, e.getMessage());
        }
    }
}