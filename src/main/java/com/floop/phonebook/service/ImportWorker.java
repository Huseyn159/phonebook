package com.floop.phonebook.service;

import com.floop.phonebook.dto.*;
import com.floop.phonebook.entity.PhonebookEntry;
import com.floop.phonebook.enums.ResultSetStatus;
import com.floop.phonebook.enums.RowOutcome;
import com.floop.phonebook.exception.BadRequestException;
import com.floop.phonebook.repository.PhonebookRepository;
import com.floop.phonebook.repository.PhonebookResultSetRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;


@Slf4j
@Component
public class ImportWorker {

    private final PhonebookService phonebookService;
    private final PhonebookRepository phonebookRepository;
    private final Validator validator;
    private final PhonebookResultSetRepository resultSetRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final EmailService emailService;

    public ImportWorker(PhonebookService phonebookService,
                        PhonebookRepository phonebookRepository,
                        Validator validator,
                        PhonebookResultSetRepository resultSetRepository, SimpMessagingTemplate messagingTemplate, EmailService emailService) {
        this.phonebookService = phonebookService;
        this.phonebookRepository = phonebookRepository;
        this.validator = validator;
        this.resultSetRepository = resultSetRepository;
        this.messagingTemplate = messagingTemplate;
        this.emailService = emailService;
    }

    public ImportResponse processExcelStream(InputStream is) {
        ImportResponse response = new ImportResponse();
        try (Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                if (row.getRowNum() == 0 || isRowEmpty(row)) {
                    continue;
                }
                response.setTotalRows(response.getTotalRows() + 1);
                int excelRowNumber = row.getRowNum() + 1;
                try {
                    RowOutcome outcome = processRow(row);
                    if (outcome == RowOutcome.CREATED) {
                        response.setCreated(response.getCreated() + 1);
                    } else {
                        response.setUpdated(response.getUpdated() + 1);
                    }
                } catch (Exception e) {
                    response.setFailed(response.getFailed() + 1);
                    response.getErrors().add(new ImportError(excelRowNumber, e.getMessage()));
                }
            }
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Could not read file: " + e.getMessage());
        }
        return response;
    }

    public void finalizeResultSet(UUID resultSetId, ResultSetStatus status, ImportResponse response, String errorMessage, String email) {
        var resultSet = resultSetRepository.findById(resultSetId).orElseThrow();
        resultSet.setStatus(status);
        resultSet.setFinishedAt(LocalDateTime.now());
        if (response != null) {
            resultSet.setTotalRows(response.getTotalRows());
            resultSet.setCreatedCount(response.getCreated());
            resultSet.setUpdatedCount(response.getUpdated());
            resultSet.setFailedCount(response.getFailed());
            if (response.getErrors() != null && !response.getErrors().isEmpty()) {
                StringBuilder summary = new StringBuilder();

                for (int i = 0; i < response.getErrors().size() && i < 10; i++) {
                    ImportError error = response.getErrors().get(i);
                    summary.append("Row ").append(error.getRow())
                            .append(": ").append(error.getMessage())
                            .append("\n");
                }

                resultSet.setErrorSummary(summary.toString());
            }
        }
        if (errorMessage != null) {
            resultSet.setErrorSummary(errorMessage);
        }
        resultSetRepository.save(resultSet);
        messagingTemplate.convertAndSend("/topic/result-set/" + resultSetId, resultSet);

        if (email != null && !email.isBlank()) {
           try {
               emailService.sendImportCompletedEmail(email, resultSet);
           }catch (MailException e) {
               log.warn("Could not send email for result set {}", resultSetId, e);
           }
        }

    }

    private RowOutcome processRow(Row row) {
        PhonebookEntryRequest request = buildRequest(row);

        Set<ConstraintViolation<PhonebookEntryRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            String message = violations.stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.joining("; "));
            throw new BadRequestException(message);
        }

        Optional<PhonebookEntry> existing = phonebookRepository
                .findByNumberAndActivatedDate(request.getNumber(), request.getActivatedDate());

        if (existing.isPresent()) {
            phonebookService.update(existing.get().getId(), request);
            return RowOutcome.UPDATED;
        } else {
            phonebookService.create(request);
            return RowOutcome.CREATED;
        }
    }

    private PhonebookEntryRequest buildRequest(Row row) {
        PhonebookEntryRequest request = new PhonebookEntryRequest();
        request.setName(capitalize(readString(row, 0)));
        request.setSurname(capitalize(readString(row, 1)));
        request.setNationalId(readString(row, 2));
        request.setDateOfBirth(readLocalDate(row, 3));
        request.setFin(readString(row, 4));
        request.setAddress(readString(row, 5));
        request.setCity(capitalize(readString(row, 6)));
        request.setNumber(readString(row, 7));
        request.setActivatedDate(readLocalDate(row, 8));
        request.setStopDate(readLocalDate(row, 9));
        return request;
    }

    private String readString(Row row, int index) {
        Cell cell = row.getCell(index);
        return cell == null ? null : cell.getStringCellValue().trim();
    }

    private LocalDate readLocalDate(Row row, int index) {
        Cell cell = row.getCell(index);
        return cell == null ? null : cell.getLocalDateTimeCellValue().toLocalDate();
    }

    private String capitalize(String value) {
        if (value == null || value.isBlank()) return value;
        String[] words = value.trim().split("\\s+");
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            result.append(Character.toUpperCase(words[i].charAt(0)))
                    .append(words[i].substring(1).toLowerCase());
            if (i < words.length - 1) result.append(" ");
        }
        return result.toString();
    }

    private boolean isRowEmpty(Row row) {
        return readString(row, 0) == null && readString(row, 1) == null;
    }
}