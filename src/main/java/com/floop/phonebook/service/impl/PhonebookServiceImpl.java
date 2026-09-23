package com.floop.phonebook.service.impl;

import com.floop.phonebook.dto.PhonebookEntryRequest;
import com.floop.phonebook.dto.PhonebookEntryResponse;
import com.floop.phonebook.entity.PhonebookEntry;
import com.floop.phonebook.exception.BadRequestException;
import com.floop.phonebook.exception.ResourceNotFoundException;
import com.floop.phonebook.repository.PhonebookRepository;
import com.floop.phonebook.service.PhonebookService;
import org.springframework.stereotype.Service;


import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PhonebookServiceImpl implements PhonebookService {

    private final PhonebookRepository repository;

    public PhonebookServiceImpl(PhonebookRepository repository) {
        this.repository = repository;
    }

    @Override
    public PhonebookEntryResponse create(PhonebookEntryRequest request) {
        boolean active = request.getStopDate() == null;

        validateStopNotBeforeActivated(request.getActivatedDate(), request.getStopDate());
        validateActiveNumberConstraint(request.getNumber(), request.getActivatedDate(), active, null);

        PhonebookEntry entry = new PhonebookEntry();
        entry.setName(request.getName());
        entry.setSurname(request.getSurname());
        entry.setNationalId(request.getNationalId());
        entry.setDateOfBirth(request.getDateOfBirth());
        entry.setFin(request.getFin());
        entry.setAddress(request.getAddress());
        entry.setCity(request.getCity());
        entry.setNumber(request.getNumber());
        entry.setActivatedDate(request.getActivatedDate());
        entry.setStopDate(request.getStopDate());
        entry.setActive(active);

        PhonebookEntry saved = repository.save(entry);

        return toResponse(saved);
    }

    @Override
    public PhonebookEntryResponse getById(UUID id) {
        return repository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(id));
    }

    @Override
    public PhonebookEntryResponse update(UUID id, PhonebookEntryRequest request) {
        PhonebookEntry entry = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(id));

        boolean active = request.getStopDate() == null;

        validateStopNotBeforeActivated(request.getActivatedDate(), request.getStopDate());
        validateActiveNumberConstraint(request.getNumber(), request.getActivatedDate(), active, id);

        entry.setName(request.getName());
        entry.setSurname(request.getSurname());
        entry.setNationalId(request.getNationalId());
        entry.setDateOfBirth(request.getDateOfBirth());
        entry.setFin(request.getFin());
        entry.setAddress(request.getAddress());
        entry.setCity(request.getCity());
        entry.setNumber(request.getNumber());
        entry.setActivatedDate(request.getActivatedDate());
        entry.setStopDate(request.getStopDate());
        entry.setActive(active);

        PhonebookEntry saved = repository.save(entry);

        return toResponse(saved);
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

    private PhonebookEntryResponse toResponse(PhonebookEntry e) {
        PhonebookEntryResponse response = new PhonebookEntryResponse();
        response.setId(e.getId());
        response.setName(e.getName());
        response.setSurname(e.getSurname());
        response.setNationalId(e.getNationalId());
        response.setDateOfBirth(e.getDateOfBirth());
        response.setFin(e.getFin());
        response.setAddress(e.getAddress());
        response.setCity(e.getCity());
        response.setNumber(e.getNumber());
        response.setActivatedDate(e.getActivatedDate());
        response.setStopDate(e.getStopDate());
        response.setActive(e.isActive());
        response.setCreatedAt(e.getCreatedAt());
        response.setUpdatedAt(e.getUpdatedAt());
        return response;
    }
}