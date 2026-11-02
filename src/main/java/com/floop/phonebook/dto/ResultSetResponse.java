package com.floop.phonebook.dto;

import com.floop.phonebook.enums.ResultSetStatus;
import com.floop.phonebook.enums.ResultSetType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;


@Getter
@Setter
public class ResultSetResponse {
        @Id
        @GeneratedValue
        private UUID id;

        @Column(name = "type")
        @Enumerated(EnumType.STRING)
        private ResultSetType type;

        @Column(name = "status")
        @Enumerated(EnumType.STRING)
        private ResultSetStatus status;

        @Column(name = "total_rows")
        private int totalRows;

        @Column(name = "expiration_time")
        private LocalDateTime expirationTime; //scheduler ucun.Snapshotun omrunun sonu

        @Column(name = "updated_count")
        private int updatedCount;

        @Column(name = "created_count")
        private int createdCount;

        @Column(name = "failed_count")
        private int failedCount;

        @Column(name = "error_summary")
        private String errorSummary;

        @Column(name = "created_at")
        private LocalDateTime createdAt;

        @Column(name = "finished_at",nullable = true)
        private LocalDateTime finishedAt;

        @Column(name = "truncated")
        private boolean truncated;
}
