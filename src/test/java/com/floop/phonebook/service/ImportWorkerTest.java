package com.floop.phonebook.service;

import com.floop.phonebook.dto.ImportError;
import com.floop.phonebook.dto.ImportResponse;
import com.floop.phonebook.dto.PhonebookEntryRequest;
import com.floop.phonebook.entity.PhonebookEntry;
import com.floop.phonebook.exception.BadRequestException;
import com.floop.phonebook.repository.PhonebookRepository;
import com.floop.phonebook.repository.PhonebookResultSetRepository;
import jakarta.validation.Validator;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImportWorkerTest {


    @Mock private  DataFormatter formatter = new DataFormatter();
    @Mock private  PhonebookService phonebookService;
    @Mock private  PhonebookRepository phonebookRepository;
    @Mock private  Validator validator;
    @Mock private  PhonebookResultSetRepository resultSetRepository;
    @Mock private  SimpMessagingTemplate messagingTemplate;
    @Mock private  EmailService emailService;

    @InjectMocks private ImportWorker worker;


    private InputStream excelWithRow(Object... values) throws IOException{
        try(Workbook wb = new XSSFWorkbook()){
            Sheet sheet = wb.createSheet();
            sheet.createRow(0).createCell(0).setCellValue("header");
            Row row = sheet.createRow(1);
            for (int i = 0; i < values.length; i++) {
                if (values[i]==null) continue;
                Cell cell = row.createCell(i);
                if (values[i] instanceof LocalDate date){
                    cell.setCellValue(date);
                    CellStyle style = wb.createCellStyle();
                    style.setDataFormat(wb.getCreationHelper().createDataFormat().getFormat("yyyy-mm-dd"));
                    cell.setCellStyle(style);
                }
                else {
                    cell.setCellValue(values[i].toString());
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }


    @Test
    void  processExcelStream_whenDateIsText_parsesDate() throws IOException {
        InputStream file = excelWithRow("Ali","Veliyev",null,"1998-01-01");


        ImportResponse response = worker.processExcelStream(file);

        //Assert 1- one row created an 0 failed
        assertThat(response.getCreated()).isEqualTo(1);
        assertThat(response.getFailed()).isEqualTo(0);

        //Assert 2- is it correctly parsed and went to create?
        ArgumentCaptor<PhonebookEntryRequest> captor = ArgumentCaptor.forClass(PhonebookEntryRequest.class);
        verify(phonebookService).create(captor.capture());
        assertThat(captor.getValue().getDateOfBirth()).isEqualTo(LocalDate.of(1998,1,1));

    }

    @Test
    void processExcelStream_whenDateIsLocalDate_createsAndSaveToDb() throws IOException {
        InputStream file = excelWithRow("Ali","Veliyev",null,LocalDate.of(1998,1,1));

        ImportResponse response = worker.processExcelStream(file);

        //is it created
        assertThat(response.getCreated()).isEqualTo(1);
        assertThat(response.getFailed()).isEqualTo(0);

        //is data correctly passed to db
        ArgumentCaptor<PhonebookEntryRequest> captor = ArgumentCaptor.forClass(PhonebookEntryRequest.class);
        verify(phonebookService).create(captor.capture());
        assertThat(captor.getValue().getDateOfBirth()).isEqualTo(LocalDate.of(1998,1,1));

    }

    @Test
    void processExcelStream_whenDateFormatIsInvalid_returnsFailedCountAndErrorSummary() throws IOException {
        InputStream file = excelWithRow("Ali","Veliyev",null,"1998.01.01");

        ImportResponse response = worker.processExcelStream(file);
        ImportError error = response.getErrors().get(0);

        //is it failed and not created?
        assertThat(response.getFailed()).isEqualTo(1);
        assertThat(response.getCreated()).isEqualTo(0);
        verify(phonebookService,never()).create(any());
        assertThat(error.getRow()).isEqualTo(2);
        assertThat(error.getMessage()).contains("dateOfBirth");
    }

    @Test
    void processExcelStream_whenRowIsEmpty_skipsRow() throws IOException {
        InputStream file = excelWithRow();

        ImportResponse response = worker.processExcelStream(file);

        assertThat(response.getTotalRows()).isEqualTo(0);
        verifyNoInteractions(phonebookService);
    }

    @Test
    void processExcelStream_whenEntryExists_updatesIt() throws IOException {
        InputStream file = excelWithRow("Ali");
        UUID id = UUID.randomUUID();
        PhonebookEntry existing = new PhonebookEntry();
        existing.setId(id);
        when(phonebookRepository.findByNumberAndActivatedDate(any(),any())).thenReturn(Optional.of(existing));


        ImportResponse response = worker.processExcelStream(file);

        assertThat(response.getUpdated()).isEqualTo(1);
        verify(phonebookService).update(eq(id),any());
        verify(phonebookService,never()).create(any());
    }

    @Test
    void processExcelStream_whenFileIsNotExcel_throwsBadRequest(){
        InputStream file = new ByteArrayInputStream("abc".getBytes());

        assertThatThrownBy(()->worker.processExcelStream(file)).isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Could not read file");
    }
}