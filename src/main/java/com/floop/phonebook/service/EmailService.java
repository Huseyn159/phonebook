package com.floop.phonebook.service;

import com.floop.phonebook.entity.PhonebookResultSet;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;


    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendImportCompletedEmail(String to, PhonebookResultSet resultSet) {

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Import Completed");
        message.setText("Your Phonebook has been imported successfully.\n" +
                "Status: " + resultSet.getStatus() + "\n" +
                "Created Count: " + resultSet.getCreatedCount() + "\n" +
                "Updated Count: " + resultSet.getUpdatedCount() + "\n" +
                "Failed Count: " + resultSet.getFailedCount());
        mailSender.send(message);


    }
}
