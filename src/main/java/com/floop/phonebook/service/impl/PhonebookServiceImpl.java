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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;


import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PhonebookServiceImpl implements PhonebookService {

    private final PhonebookMapper mapper;
    private final PhonebookRepository repository;
    private final PhonebookSpecificationBuilder specificationBuilder;


    public PhonebookServiceImpl(PhonebookMapper mapper, PhonebookRepository repository, PhonebookSpecificationBuilder specificationBuilder) {
        this.mapper = mapper;
        this.repository = repository;
        this.specificationBuilder = specificationBuilder;
    }


    @Override
    public PhonebookEntryResponse create(PhonebookEntryRequest request) {
        boolean active = request.getStopDate() == null;

        validateStopNotBeforeActivated(request.getActivatedDate(), request.getStopDate());
        validateActiveNumberConstraint(request.getNumber(), request.getActivatedDate(), active, null);

        PhonebookEntry entry = mapper.toEntity(request);
        entry.setActive(active);

        PhonebookEntry saved = repository.save(entry);
        return mapper.toResponse(saved);
    }

    @Override
    public PhonebookEntryResponse getById(UUID id) {
        return repository.findById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(id));
    }

    @Override
    public PhonebookEntryResponse update(UUID id, PhonebookEntryRequest request) {
        PhonebookEntry entry = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(id));

        boolean active = request.getStopDate() == null;

        validateStopNotBeforeActivated(request.getActivatedDate(), request.getStopDate());
        validateActiveNumberConstraint(request.getNumber(), request.getActivatedDate(), active, id);

        mapper.updateEntityFromRequest(request, entry);
        entry.setActive(active);

        PhonebookEntry saved = repository.save(entry);

        return mapper.toResponse(saved);
    }

    @Override
    public Page<PhonebookEntryResponse> search(SearchRequest searchRequest, Pageable pageable) {
        Specification<PhonebookEntry> spec = specificationBuilder.buildAll(searchRequest);

        return repository.findAll(spec, pageable)
                .map(mapper::toResponse);
    }

    @Override
    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException(id);
        }
        repository.deleteById(id);
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
            List<PhonebookEntry> activeRows = repository.findByNumberAndActiveTrue(number);

            Optional<PhonebookEntry> conflict = activeRows.stream()
                    .filter(e -> !e.getId().equals(excludeId))
                    .findFirst();

            if (conflict.isPresent()) {
                throw new BadRequestException(
                        "You need to set stop date for the data with id " + conflict.get().getId() + " first");
            }

            List<PhonebookEntry> stopped = repository.findByNumberAndStopDateIsNotNullOrderByStopDateDesc(number);

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