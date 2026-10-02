package com.floop.phonebook.service.impl;

import com.floop.phonebook.dto.ImportError;
import com.floop.phonebook.dto.ImportResponse;
import com.floop.phonebook.dto.PhonebookEntryRequest;
import com.floop.phonebook.entity.PhonebookEntry;
import com.floop.phonebook.entity.PhonebookResultSet;
import com.floop.phonebook.entity.UserEntity;
import com.floop.phonebook.enums.ResultSetStatus;
import com.floop.phonebook.enums.ResultSetType;
import com.floop.phonebook.exception.BadRequestException;
import com.floop.phonebook.repository.PhonebookRepository;
import com.floop.phonebook.repository.PhonebookResultSetRepository;
import com.floop.phonebook.repository.UserRepository;
import com.floop.phonebook.service.ImportProcessingService;
import com.floop.phonebook.service.ImportWorker;
import com.floop.phonebook.service.PhonebookService;
import jakarta.validation.ConstraintViolation;
import org.apache.poi.ss.usermodel.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ImportProcessingServiceImpl implements ImportProcessingService {

    private final ImportWorker importWorker;
    private final AsyncImportProcessor asyncImportProcessor;
    private final PhonebookResultSetRepository resultSetRepository;
    private final UserRepository userRepository;

    public ImportProcessingServiceImpl(ImportWorker importWorker,
                                       AsyncImportProcessor asyncImportProcessor,
                                       PhonebookResultSetRepository resultSetRepository, UserRepository userRepository) {
        this.importWorker = importWorker;
        this.asyncImportProcessor = asyncImportProcessor;
        this.resultSetRepository = resultSetRepository;
        this.userRepository = userRepository;
    }

    @Override
    public ImportResponse importFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }
        try (InputStream is = file.getInputStream()) {
            return importWorker.processExcelStream(is);
        } catch (IOException e) {
            throw new BadRequestException("Could not read file: " + e.getMessage());
        }
    }

    @Override
    public PhonebookResultSet importAsync(MultipartFile file) throws IOException {
        String path = saveFileToDisk(file);
        PhonebookResultSet resultSet = createInProgressResultSet(path);

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        String email = userRepository.findByUsername(username)
                        .map(UserEntity::getEmail)
                        .orElse(null);

        asyncImportProcessor.processImportAsync(resultSet.getId(), path,email);
        return resultSet;
    }

    private String saveFileToDisk(MultipartFile file) throws IOException {
        String uploadDir = "uploads/";
        Files.createDirectories(Paths.get(uploadDir));
        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path filePath = Paths.get(uploadDir, fileName);
        file.transferTo(filePath);
        return filePath.toString();
    }

    private PhonebookResultSet createInProgressResultSet(String filePath) {
        PhonebookResultSet resultSet = new PhonebookResultSet();
        resultSet.setType(ResultSetType.IMPORT);
        resultSet.setStatus(ResultSetStatus.IN_PROGRESS);
        resultSet.setFilePath(filePath);
        resultSet.setCreatedAt(LocalDateTime.now());
        return resultSetRepository.save(resultSet);
    }
}