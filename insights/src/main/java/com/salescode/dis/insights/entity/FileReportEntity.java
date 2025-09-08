package com.salescode.dis.insights.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;

@Entity
@Table(name = "file_report")
@Data
public class FileReportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String fileId;

    @Column(length = 1024)
    private String url;

    @Column
    private String name;

    @Column()
    private String status;

    @Column(length = 1024)
    private String errorMessage;
}