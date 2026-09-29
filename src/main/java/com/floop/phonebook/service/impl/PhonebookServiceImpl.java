package com.floop.phonebook.service.impl;

import com.floop.phonebook.dto.PhonebookEntryRequest;
import com.floop.phonebook.dto.PhonebookEntryResponse;
import com.floop.phonebook.dto.SearchRequest;
import com.floop.phonebook.entity.PhonebookEntry;
import com.floop.phonebook.exception.BadRequestException;
import com.floop.phonebook.exception.ResourceNotFoundException;
import com.floop.phonebook.mapper.PhonebookMapper;
import com.floop.phonebook.repository.PhonebookRepository;
import com.floop.phonebook.search.PhonebookSpecificationBuilder;
import com.floop.phonebook.service.PhonebookService;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PhonebookServiceImpl implements PhonebookService {

    private final PhonebookMapper mapper;
    private final PhonebookRepository phonebookRepository;
    private final PhonebookSpecificationBuilder specificationBuilder;
    private static final int MAX_EXPORT_ROWS = 50_000;

    public PhonebookServiceImpl(PhonebookMapper mapper, PhonebookRepository phonebookRepository, PhonebookSpecificationBuilder specificationBuilder) {
        this.mapper = mapper;
        this.phonebookRepository = phonebookRepository;
        this.specificationBuilder = specificationBuilder;
    }


    @Override
    public PhonebookEntryResponse create(PhonebookEntryRequest request) {
        boolean active = request.getStopDate() == null;

        validateStopNotBeforeActivated(request.getActivatedDate(), request.getStopDate());
        validateActiveNumberConstraint(request.getNumber(), request.getActivatedDate(), active, null);

        PhonebookEntry entry = mapper.toEntity(request);
        entry.setActive(active);

        PhonebookEntry saved = phonebookRepository.save(entry);
        return mapper.toResponse(saved);
    }

    @Override
    public PhonebookEntryResponse getById(UUID id) {
        return phonebookRepository.findById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(id));
    }

    @Override
    public PhonebookEntryResponse update(UUID id, PhonebookEntryRequest request) {
        PhonebookEntry entry = phonebookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(id));

        boolean active = request.getStopDate() == null;

        validateStopNotBeforeActivated(request.getActivatedDate(), request.getStopDate());
        validateActiveNumberConstraint(request.getNumber(), request.getActivatedDate(), active, id);

        mapper.updateEntityFromRequest(request, entry);
        entry.setActive(active);

        PhonebookEntry saved = phonebookRepository.save(entry);

        return mapper.toResponse(saved);
    }

    @Override
    public Page<PhonebookEntryResponse> search(SearchRequest searchRequest, Pageable pageable) {
        Specification<PhonebookEntry> spec = specificationBuilder.buildAll(searchRequest);

        return phonebookRepository.findAll(spec, pageable)
                .map(mapper::toResponse);
    }

    @Override
    public byte[] export(SearchRequest searchRequest) throws IOException  {
        Pageable cap = PageRequest.of(0, MAX_EXPORT_ROWS);


        Specification<PhonebookEntry> spec = specificationBuilder.buildAll(searchRequest);
        List<PhonebookEntry> responses = phonebookRepository.findAll(spec,cap).getContent();
        List<PhonebookEntryResponse> responseList = responses.stream()
                .map(mapper::toResponse)
                .toList();

        Workbook workbook = buildExportWorkbook(responseList);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            workbook.write(outputStream);
            workbook.close();



        return outputStream.toByteArray();
    }

    @Override
    public void delete(UUID id) {
        if (!phonebookRepository.existsById(id)) {
            throw new ResourceNotFoundException(id);
        }
        phonebookRepository.deleteById(id);
    }



    // -----------EXPORT---------------
    private String cellText(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private Workbook buildExportWorkbook(List<PhonebookEntryResponse> responses) {
            Workbook workbook = new XSSFWorkbook();

            Sheet sheet = workbook.createSheet("Phonebook");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("ID");
            header.createCell(1).setCellValue("Name");
            header.createCell(2).setCellValue("Surname");
            header.createCell(3).setCellValue("NationalId");
            header.createCell(4).setCellValue("DateOfBirth");
            header.createCell(5).setCellValue("Fin");
            header.createCell(6).setCellValue("Address");
            header.createCell(7).setCellValue("City");
            header.createCell(8).setCellValue("Number");
            header.createCell(9).setCellValue("ActivatedDate");
            header.createCell(10).setCellValue("StopDate");
            header.createCell(11).setCellValue("Active");
            header.createCell(12).setCellValue("CreatedAt");
            header.createCell(13).setCellValue("UpdatedAt");

            for (int i = 0; i < responses.size(); i++) {
                PhonebookEntryResponse response = responses.get(i);
                Row row = sheet.createRow(i + 1);

                row.createCell(0).setCellValue(String.valueOf(response.getId()));
                row.createCell(1).setCellValue(response.getName());
                row.createCell(2).setCellValue(response.getSurname());
                row.createCell(3).setCellValue(response.getNationalId());
                row.createCell(4).setCellValue(cellText(response.getDateOfBirth()));
                row.createCell(5).setCellValue(response.getFin());
                row.createCell(6).setCellValue(response.getAddress());
                row.createCell(7).setCellValue(response.getCity());
                row.createCell(8).setCellValue(response.getNumber());
                row.createCell(9).setCellValue(String.valueOf(response.getActivatedDate()));
                row.createCell(10).setCellValue(cellText(response.getStopDate()));
                row.createCell(11).setCellValue(response.isActive());
                row.createCell(12).setCellValue(String.valueOf(response.getCreatedAt()));
                row.createCell(13).setCellValue(String.valueOf(response.getUpdatedAt()));
            }


            return workbook;
    }


    private void validateStopNotBeforeActivated(LocalDate activatedDate, LocalDate stopDate) {
        if (stopDate != null && activatedDate != null && stopDate.isBefore(activatedDate)) {
            throw new BadRequestException("stopDate must not be earlier than activatedDate");
        }
    }

    private void validateActiveNumberConstraint(String number, LocalDate activatedDate,
                                                boolean active, UUID excludeId) {
        if (number == null || number.isBlank()) {
            return;
        }

        if (active) {
            List<PhonebookEntry> activeRows = phonebookRepository.findByNumberAndActiveTrue(number);

            Optional<PhonebookEntry> conflict = activeRows.stream()
                    .filter(e -> !e.getId().equals(excludeId))
                    .findFirst();

            if (conflict.isPresent()) {
                throw new BadRequestException(
                        "You need to set stop date for the data with id " + conflict.get().getId() + " first");
            }

            List<PhonebookEntry> stopped = phonebookRepository.findByNumberAndStopDateIsNotNullOrderByStopDateDesc(number);

            List<PhonebookEntry> otherStopped = stopped.stream()
                    .filter(e -> !e.getId().equals(excludeId))
                    .toList();

            if (!otherStopped.isEmpty()) {
                LocalDate lastStopDate = otherStopped.get(0).getStopDate();

                if (activatedDate == null || activatedDate.isBefore(lastStopDate)) {
                    throw new BadRequestException(
                            "activatedDate must not be earlier than the previous stopDate (" + lastStopDate + ") for this number");
                }
            }
        }
    }





}