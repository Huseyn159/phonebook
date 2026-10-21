package com.floop.phonebook.service;

import com.floop.phonebook.entity.PhonebookResultSet;
import com.floop.phonebook.enums.ResultSetStatus;
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

        if (resultSet.getStatus() == ResultSetStatus.COMPLETED) {
            message.setSubject("Import Completed");
            message.setText("Your phonebook import has completed.\n" +
                    "Total rows: " + resultSet.getTotalRows() + "\n" +
                    "Created: " + resultSet.getCreatedCount() + "\n" +
                    "Updated: " + resultSet.getUpdatedCount() + "\n" +
                    "Failed: " + resultSet.getFailedCount());
        } else {
            message.setSubject("Import Failed");
            message.setText("Your phonebook import failed.\n" +
                    "Reason: " + resultSet.getErrorSummary());
        }

        mailSender.send(message);
    }
}
