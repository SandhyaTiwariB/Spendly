package com.spendly.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class ImportSummaryResponse {
    private Long importId;
    private String filename;
    private String fileType;
    private String status;
    private int totalRows;
    private int acceptedCount;
    private int duplicateCount;
    private int rejectedCount;
    private String validationWarnings;
    private LocalDateTime importTimestamp;
    private BigDecimal totalInflow;
    private BigDecimal totalOutflow;
    private LocalDate startDate;
    private LocalDate endDate;
    private String topCategory;
    private List<TransactionResponse> sampleImportedTransactions;

    public ImportSummaryResponse() {
        this.importTimestamp = LocalDateTime.now();
    }

    public Long getImportId() {
        return importId;
    }

    public void setImportId(Long importId) {
        this.importId = importId;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
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

    public int getAcceptedCount() {
        return acceptedCount;
    }

    public void setAcceptedCount(int acceptedCount) {
        this.acceptedCount = acceptedCount;
    }

    public int getDuplicateCount() {
        return duplicateCount;
    }

    public void setDuplicateCount(int duplicateCount) {
        this.duplicateCount = duplicateCount;
    }

    public int getRejectedCount() {
        return rejectedCount;
    }

    public void setRejectedCount(int rejectedCount) {
        this.rejectedCount = rejectedCount;
    }

    public String getValidationWarnings() {
        return validationWarnings;
    }

    public void setValidationWarnings(String validationWarnings) {
        this.validationWarnings = validationWarnings;
    }

    public LocalDateTime getImportTimestamp() {
        return importTimestamp;
    }

    public void setImportTimestamp(LocalDateTime importTimestamp) {
        this.importTimestamp = importTimestamp;
    }

    public BigDecimal getTotalInflow() {
        return totalInflow;
    }

    public void setTotalInflow(BigDecimal totalInflow) {
        this.totalInflow = totalInflow;
    }

    public BigDecimal getTotalOutflow() {
        return totalOutflow;
    }

    public void setTotalOutflow(BigDecimal totalOutflow) {
        this.totalOutflow = totalOutflow;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getTopCategory() {
        return topCategory;
    }

    public void setTopCategory(String topCategory) {
        this.topCategory = topCategory;
    }

    public List<TransactionResponse> getSampleImportedTransactions() {
        return sampleImportedTransactions;
    }

    public void setSampleImportedTransactions(List<TransactionResponse> sampleImportedTransactions) {
        this.sampleImportedTransactions = sampleImportedTransactions;
    }
}
