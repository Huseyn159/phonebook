package com.floop.phonebook.entity;


import com.floop.phonebook.enums.ResultSetStatus;
import com.floop.phonebook.enums.ResultSetType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "phonebook_result_set")
@Getter
@Setter
public class PhonebookResultSet {


    @Id
    @GeneratedValue
    private UUID id;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private ResultSetType type;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private ResultSetStatus status;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "total_rows")
    private int totalRows;

    //ES uchun yeni fieldler
    @Column(name = "index_name")
    private String indexName;

    @Column(name = "criteria_json")
    private String criteriaJson; //axtarisda ishledilmir yalniz qeyd ucun.Neyin sureti oldugunu anlamaq meqsedile

    @Column(name = "expiration_time")
    private LocalDateTime expirationTime;

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
