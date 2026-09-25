package com.floop.phonebook.mapper;

import com.floop.phonebook.dto.PhonebookEntryRequest;
import com.floop.phonebook.dto.PhonebookEntryResponse;
import com.floop.phonebook.entity.PhonebookEntry;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-09-24T16:39:22+0400",
    comments = "version: 1.6.3, compiler: javac, environment: Java 21.0.10 (Homebrew)"
)
@Component
public class PhonebookMapperImpl implements PhonebookMapper {

    @Override
    public PhonebookEntry toEntity(PhonebookEntryRequest request) {
        if ( request == null ) {
            return null;
        }

        PhonebookEntry phonebookEntry = new PhonebookEntry();

        phonebookEntry.setName( request.getName() );
        phonebookEntry.setSurname( request.getSurname() );
        phonebookEntry.setNationalId( request.getNationalId() );
        phonebookEntry.setDateOfBirth( request.getDateOfBirth() );
        phonebookEntry.setFin( request.getFin() );
        phonebookEntry.setAddress( request.getAddress() );
        phonebookEntry.setCity( request.getCity() );
        phonebookEntry.setNumber( request.getNumber() );
        phonebookEntry.setActivatedDate( request.getActivatedDate() );
        phonebookEntry.setStopDate( request.getStopDate() );

        return phonebookEntry;
    }

    @Override
    public void updateEntityFromRequest(PhonebookEntryRequest request, PhonebookEntry entity) {
        if ( request == null ) {
            return;
        }

        entity.setName( request.getName() );
        entity.setSurname( request.getSurname() );
        entity.setNationalId( request.getNationalId() );
        entity.setDateOfBirth( request.getDateOfBirth() );
        entity.setFin( request.getFin() );
        entity.setAddress( request.getAddress() );
        entity.setCity( request.getCity() );
        entity.setNumber( request.getNumber() );
        entity.setActivatedDate( request.getActivatedDate() );
        entity.setStopDate( request.getStopDate() );
    }

    @Override
    public PhonebookEntryResponse toResponse(PhonebookEntry entity) {
        if ( entity == null ) {
            return null;
        }

        PhonebookEntryResponse phonebookEntryResponse = new PhonebookEntryResponse();

        phonebookEntryResponse.setId( entity.getId() );
        phonebookEntryResponse.setName( entity.getName() );
        phonebookEntryResponse.setSurname( entity.getSurname() );
        phonebookEntryResponse.setNationalId( entity.getNationalId() );
        phonebookEntryResponse.setDateOfBirth( entity.getDateOfBirth() );
        phonebookEntryResponse.setFin( entity.getFin() );
        phonebookEntryResponse.setAddress( entity.getAddress() );
        phonebookEntryResponse.setCity( entity.getCity() );
        phonebookEntryResponse.setNumber( entity.getNumber() );
        phonebookEntryResponse.setActivatedDate( entity.getActivatedDate() );
        phonebookEntryResponse.setStopDate( entity.getStopDate() );
        phonebookEntryResponse.setActive( entity.isActive() );
        phonebookEntryResponse.setCreatedAt( entity.getCreatedAt() );
        phonebookEntryResponse.setUpdatedAt( entity.getUpdatedAt() );

        return phonebookEntryResponse;
    }
}
