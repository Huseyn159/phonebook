package com.floop.phonebook.service.impl;

import com.floop.phonebook.dto.PhonebookEntryRequest;
import com.floop.phonebook.dto.PhonebookEntryResponse;
import com.floop.phonebook.entity.PhonebookEntry;
import com.floop.phonebook.exception.BadRequestException;
import com.floop.phonebook.exception.ConflictException;
import com.floop.phonebook.exception.ResourceNotFoundException;
import com.floop.phonebook.mapper.PhonebookMapper;
import com.floop.phonebook.repository.PhonebookRepository;
import com.floop.phonebook.search.PhonebookSpecificationBuilder;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PhonebookServiceImplTest {

    @Mock
    PhonebookRepository phonebookRepository;
    @Mock
    PhonebookMapper mapper;
    @Mock
    PhonebookSpecificationBuilder specificationBuilder;
    @Mock
    EntityManager entityManager;

    @InjectMocks PhonebookServiceImpl service;



    //===========================================CREATE TESTS===========================================================

    @Test
    void create_whenNumberAlreadyHasActiveEntry_throwsBadRequest(){
        //Arrange
        PhonebookEntry existing = new PhonebookEntry();
        existing.setId(UUID.randomUUID());
        when(phonebookRepository.findByNumberAndActiveTrue("0501234567"))
                .thenReturn(List.of(existing));

        PhonebookEntryRequest request = new PhonebookEntryRequest();
        request.setNumber("0501234567");
        request.setActivatedDate(LocalDate.of(2026,1,1));

        //Act + Assert
        assertThatThrownBy(()-> service.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("set stop date");
    }

    @Test
    void create_whenStopDateIsBeforeActivatedDate_throwsBadRequest(){
        //Arrange
        PhonebookEntryRequest request = new PhonebookEntryRequest();
        request.setStopDate(LocalDate.of(2025,1,1));
        request.setActivatedDate(LocalDate.of(2026,1,1));


        //Act+Assert
        assertThatThrownBy(()->service.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("earlier than activatedDate");
        verify(phonebookRepository, never()).save(any());

    }

    @Test
    void create_whenActivatedDateIsBeforePreviousStopDate_throwsBadRequest() {
        // Arrange

        PhonebookEntry previous = new PhonebookEntry();
        previous.setStopDate(LocalDate.of(2025,1,1));
        previous.setId(UUID.randomUUID());

        PhonebookEntryRequest request = new PhonebookEntryRequest();
        request.setActivatedDate(LocalDate.of(2024,1,1));
        request.setNumber("0501234567");

        when(phonebookRepository.findByNumberAndStopDateIsNotNullOrderByStopDateDesc("0501234567")).thenReturn(List.of(previous));


        //Act + Assert
        assertThatThrownBy(()->service.create(request)).isInstanceOf(BadRequestException.class).hasMessageContaining("previous stopDate");

        verify(phonebookRepository,never()).save(any());

    }

    @Test
    void create_whenSaveViolatesUniqueIndex_throwsConflict(){
        //Arrange
        PhonebookEntryRequest request = new PhonebookEntryRequest();
        request.setNumber("0501234567");
        request.setActivatedDate(LocalDate.of(2026, 1, 1));

        PhonebookEntry entry = new PhonebookEntry();

        when(mapper.toEntity(request)).thenReturn(entry);
        when(phonebookRepository.save(any()))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        //Act + assert
        assertThatThrownBy(()->service.create(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("0501234567");

    }



    //===============================Success Cases=========================================
    @Test
    void create_whenActivatedDateIsEqualsPreviousStopDate_savesRequestAndReturnResponse(){

        //Arrange
        PhonebookEntry previous = new PhonebookEntry();
        previous.setId(UUID.randomUUID());
        previous.setStopDate(LocalDate.of(2026,1,1));

        PhonebookEntryRequest request = new PhonebookEntryRequest();
        request.setNumber("0501234567");
        request.setActivatedDate(LocalDate.of(2026,1,1));

        PhonebookEntry newEntry = new PhonebookEntry();

        when(phonebookRepository
                .findByNumberAndStopDateIsNotNullOrderByStopDateDesc("0501234567"))
                .thenReturn(List.of(previous));

        when(mapper.toEntity(request)).thenReturn(newEntry);
        when(phonebookRepository.save(any())).thenReturn(newEntry);

        //Act + Assert
        service.create(request);
        verify(phonebookRepository).save(any());




    }

    @Test
    void create_whenNoOtherActiveEntry_savesActiveAndReturnsResponse(){
        //Arrange
        PhonebookEntryRequest request = new PhonebookEntryRequest();
        request.setNumber("0501234567");
        request.setActivatedDate(LocalDate.of(2026,1,1));

        PhonebookEntry entity = new PhonebookEntry();
        PhonebookEntryResponse response = new PhonebookEntryResponse();

        when(mapper.toEntity(request)).thenReturn(entity);
        when(phonebookRepository.save(any())).thenReturn(entity);
        when(mapper.toResponse(entity)).thenReturn(response);

        //Act
        PhonebookEntryResponse result = service.create(request);

        //Assert
        ArgumentCaptor<PhonebookEntry> captor = ArgumentCaptor.forClass(PhonebookEntry.class);
        verify(phonebookRepository).save(captor.capture());
        assertThat(captor.getValue().isActive()).isTrue();

        assertThat(result).isSameAs(response);
    }

    @Test
    void create_whenStopDateIsSet_savesInactiveWithoutActiveNumberCheck(){
        //Arrange
        PhonebookEntryRequest request = new PhonebookEntryRequest();
        request.setNumber("0501234567");
        request.setStopDate(LocalDate.of(2026,1,1));
        request.setActivatedDate(LocalDate.of(2025,1,1));

        PhonebookEntry entry = new PhonebookEntry();

        when(mapper.toEntity(request)).thenReturn(entry);

        //Act + Assert

        ArgumentCaptor<PhonebookEntry> captor = ArgumentCaptor.forClass(PhonebookEntry.class);

        service.create(request);
        verify(phonebookRepository).save(captor.capture());
        assertThat(captor.getValue().isActive()).isFalse();
        verify(phonebookRepository,never()).findByNumberAndActiveTrue(any());

    }





    //===========================================UPDATE TESTS===========================================================

    @Test
    void update_whenIdNotFound_throwsResourceNotFound(){
        //Arrange
        PhonebookEntryRequest request = new PhonebookEntryRequest();
        UUID id = UUID.randomUUID();

        //Act+Assert
        assertThatThrownBy(()->service.update(id,request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(id.toString());

        verify(phonebookRepository,never()).save(any());
    }

    @Test
    void update_whenDifferentActiveEntry_throwsBadRequest(){
        //Arrange
        UUID updatedId = UUID.randomUUID();
        UUID otherActiveId = UUID.randomUUID();
        PhonebookEntry updatedEntry = new PhonebookEntry();
        updatedEntry.setId(updatedId);
        PhonebookEntry ownerEntry = new PhonebookEntry();
        ownerEntry.setId(otherActiveId);
        PhonebookEntryRequest request = new PhonebookEntryRequest();
        request.setActivatedDate(LocalDate.of(2026,1,1));
        request.setNumber("0501234567");

        when(phonebookRepository.findById(updatedId))
                .thenReturn(Optional.of(updatedEntry));
        when(phonebookRepository.findByNumberAndActiveTrue("0501234567"))
                .thenReturn(List.of(ownerEntry));


        //Act +  Assert
        assertThatThrownBy(()-> service.update(updatedId,request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining(otherActiveId.toString());
        verify(phonebookRepository,never()).save(any());
    }

    @Test
    void update_whenSaveViolatesUniqueIndex_throwsConflictException(){
       //Arrange
        UUID id = UUID.randomUUID();
        PhonebookEntry entry = new PhonebookEntry();
        entry.setId(id);
        PhonebookEntryRequest request = new PhonebookEntryRequest();
        request.setNumber("0501234567");


        when(phonebookRepository.findById(id)).thenReturn(Optional.of(entry));
        when(phonebookRepository.save(any())).thenThrow(DataIntegrityViolationException.class);

        //Act + Assert
        assertThatThrownBy(() ->service.update(id,request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining(request.getNumber());




    }


    //===============================Success Cases=========================================
    @Test
    void update_whenOnlyActiveEntryIsItself_saves(){
        //Arrange
        UUID id = UUID.randomUUID();
        PhonebookEntry entry = new PhonebookEntry();
        entry.setId(id);
        PhonebookEntryRequest request = new PhonebookEntryRequest();
        request.setNumber("0501234567");
        request.setActivatedDate(LocalDate.of(2026,1,1));



        when(phonebookRepository.findById(id)).thenReturn(Optional.of(entry));
        when(phonebookRepository.findByNumberAndActiveTrue("0501234567")).thenReturn(List.of(entry));

        //Act
        service.update(id,request);

        //Assert
        ArgumentCaptor<PhonebookEntry> captor = ArgumentCaptor.forClass(PhonebookEntry.class);
        verify(phonebookRepository).save(captor.capture());
        assertThat(captor.getValue().isActive()).isTrue();
        assertThat(captor.getValue()).isSameAs(entry);

    }

    @Test
    void update_whenStopingActiveEntry_updateEntryStatus(){
        //Arrange
        UUID id = UUID.randomUUID();
        PhonebookEntry entry = new PhonebookEntry();
        entry.setId(id);

        PhonebookEntryResponse response = new PhonebookEntryResponse();

        PhonebookEntryRequest request = new PhonebookEntryRequest();
        request.setNumber("0501234567");
        request.setActivatedDate(LocalDate.of(2025,1,1));
        request.setStopDate(LocalDate.of(2026,1,1));

        when(phonebookRepository.findById(id)).thenReturn(Optional.of(entry));
        when(phonebookRepository.save(entry)).thenReturn(entry);
        when(mapper.toResponse(entry)).thenReturn(response);

        //Act
        PhonebookEntryResponse result = service.update(id, request);

        //Assert
        ArgumentCaptor<PhonebookEntry> captor = ArgumentCaptor.forClass(PhonebookEntry.class);
        verify(phonebookRepository).save(captor.capture());
        assertThat(captor.getValue().isActive()).isFalse();
        assertThat(captor.getValue()).isSameAs(entry);
        verify(phonebookRepository,never()).findByNumberAndActiveTrue(any());
        verify(mapper).updateEntityFromRequest(request,entry);
        assertThat(result).isSameAs(response);
    }


}