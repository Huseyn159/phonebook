package com.floop.phonebook.service.impl;

import com.floop.phonebook.dto.ImportError;
import com.floop.phonebook.dto.ImportResponse;
import com.floop.phonebook.dto.PhonebookEntryRequest;
import com.floop.phonebook.entity.PhonebookEntry;
import com.floop.phonebook.exception.BadRequestException;
import com.floop.phonebook.repository.PhonebookRepository;
import com.floop.phonebook.service.ImportService;
import com.floop.phonebook.service.PhonebookService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ImportServiceImpl implements ImportService {

    private final PhonebookService phonebookService;
    private final PhonebookRepository phonebookRepository;
    private final Validator validator;

    private enum RowOutcome {
        CREATED, UPDATED
    }

    public ImportServiceImpl(PhonebookService phonebookService, PhonebookRepository phonebookRepository, Validator validator) {
        this.phonebookService = phonebookService;
        this.phonebookRepository = phonebookRepository;
        this.validator = validator;
    }






    @Override
    public ImportResponse importFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is empty");

        }

        ImportResponse response = new ImportResponse();

        try(InputStream is = file.getInputStream();
            Workbook workbook = WorkbookFactory.create(is)){

            Sheet sheet = workbook.getSheetAt(0);

            for (Row row : sheet) {
                if(row.getRowNum() == 0) {
                    continue;
                }
                if (isRowEmpty(row)) {
                    continue;
                }

                response.setTotalRows(response.getTotalRows()+1);
                int excelRowNumber = row.getRowNum()+1;

                try{
                    RowOutcome outcome = processRow(row);
                    if(outcome == RowOutcome.CREATED) {
                        response.setCreated(response.getCreated()+1);
                    }
                    else {
                        response.setUpdated(response.getUpdated()+1);
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


    private RowOutcome processRow(Row row){



        PhonebookEntryRequest request = buildRequest(row);


        Set<ConstraintViolation<PhonebookEntryRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            String message = violations.stream()
                    .map(v->v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.joining("; "));

            throw new BadRequestException(message);
        }

        Optional<PhonebookEntry> existing = phonebookRepository.findByNumberAndActivatedDate
                (request.getNumber(), request.getActivatedDate());

        if(existing.isPresent()){
            phonebookService.update(existing.get().getId(),  request);
            return RowOutcome.UPDATED;
        }
        else {
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

    private String readString(Row row,int index) {
        Cell cell = row.getCell(index);
        if (cell == null) {
            return null;
        }
        return cell.getStringCellValue().trim();
    }

    private LocalDate readLocalDate(Row row,int index) {
        Cell cell = row.getCell(index);
        if (cell == null) {
            return null;
        }
        return cell.getLocalDateTimeCellValue().toLocalDate();
    }

    private String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }

        String[] words = value.trim().split("\\s+");
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            result.append(Character.toUpperCase(word.charAt(0)))
                    .append(word.substring(1).toLowerCase());
            if (i < words.length - 1) {
                result.append(" ");
            }
        }

        return result.toString();
    }

    private boolean isRowEmpty(Row row) {
        return readString(row, 0) == null && readString(row, 1) == null;
    }
}
