package com.spendly.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "statement_imports")
public class StatementImport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String originalFilename;

    @Column(nullable = false, length = 50)
    private String fileType; // CSV, XLS, XLSX, PDF

    @Column(nullable = false)
    private LocalDateTime importTimestamp;

    @Column(nullable = false, length = 50)
    private String status; // SUCCESS, PARTIAL, FAILED

    private int totalRows;

    private int acceptedRowCount;

    private int duplicateRowCount;

    private int rejectedRowCount;

    @Column(columnDefinition = "TEXT")
    private String validationWarnings;

    public StatementImport() {
        this.importTimestamp = LocalDateTime.now();
    }

    public StatementImport(String originalFilename, String fileType, String status) {
        this.originalFilename = originalFilename;
        this.fileType = fileType;
        this.status = status;
        this.importTimestamp = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public LocalDateTime getImportTimestamp() {
        return importTimestamp;
    }

    public void setImportTimestamp(LocalDateTime importTimestamp) {
        this.importTimestamp = importTimestamp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(int totalRows) {
        this.totalRows = totalRows;
    }

    public int getAcceptedRowCount() {
        return acceptedRowCount;
    }

    public void setAcceptedRowCount(int acceptedRowCount) {
        this.acceptedRowCount = acceptedRowCount;
    }

    public int getDuplicateRowCount() {
        return duplicateRowCount;
    }

    public void setDuplicateRowCount(int duplicateRowCount) {
        this.duplicateRowCount = duplicateRowCount;
    }

    public int getRejectedRowCount() {
        return rejectedRowCount;
    }

    public void setRejectedRowCount(int rejectedRowCount) {
        this.rejectedRowCount = rejectedRowCount;
    }

    public String getValidationWarnings() {
        return validationWarnings;
    }

    public void setValidationWarnings(String validationWarnings) {
        this.validationWarnings = validationWarnings;
    }
}
