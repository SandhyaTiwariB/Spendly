package com.spendly.service;

import com.spendly.dto.ImportSummaryResponse;
import com.spendly.dto.ParsedTransactionRow;
import com.spendly.dto.TransactionResponse;
import com.spendly.entity.StatementImport;
import com.spendly.entity.Transaction;
import com.spendly.entity.TransactionType;
import com.spendly.exception.InvalidFileException;
import com.spendly.exception.ResourceNotFoundException;
import com.spendly.parser.StatementParser;
import com.spendly.parser.StatementParserFactory;
import com.spendly.repository.StatementImportRepository;
import com.spendly.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StatementImportService {

    private final StatementParserFactory parserFactory;
    private final TransactionRepository transactionRepository;
    private final StatementImportRepository importRepository;
    private final CategorizationService categorizationService;
    private final ParentControlService parentControlService;

    public StatementImportService(StatementParserFactory parserFactory,
                                  TransactionRepository transactionRepository,
                                  StatementImportRepository importRepository,
                                  CategorizationService categorizationService,
                                  ParentControlService parentControlService) {
        this.parserFactory = parserFactory;
        this.transactionRepository = transactionRepository;
        this.importRepository = importRepository;
        this.categorizationService = categorizationService;
        this.parentControlService = parentControlService;
    }

    @Transactional
    public ImportSummaryResponse importStatement(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("Please select a valid, non-empty bank statement file to upload.");
        }

        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "statement";
        String extension = getExtension(filename).toLowerCase();
        if (!List.of("csv", "xls", "xlsx", "pdf").contains(extension)) {
            throw new InvalidFileException("Unsupported file format: '" + extension + "'. Only CSV, XLS, XLSX and text-based PDF files are supported.");
        }

        StatementParser parser = parserFactory.getParser(filename, file.getContentType());
        List<ParsedTransactionRow> parsedRows;
        try (InputStream is = file.getInputStream()) {
            parsedRows = parser.parse(is, filename);
        } catch (Exception e) {
            if (e instanceof InvalidFileException) throw (InvalidFileException) e;
            throw new InvalidFileException("Failed to read file: " + e.getMessage());
        }

        if (parsedRows.isEmpty()) {
            throw new InvalidFileException("The uploaded statement did not yield any transaction records.");
        }

        // Initialize import record
        StatementImport importRecord = new StatementImport(filename, extension.toUpperCase(), "PENDING");
        importRecord.setTotalRows(parsedRows.size());
        importRecord = importRepository.save(importRecord);

        int accepted = 0;
        int duplicates = 0;
        int rejected = 0;
        List<String> warnings = new ArrayList<>();
        List<Transaction> toSave = new ArrayList<>();
        Set<String> batchFingerprints = new HashSet<>();

        for (ParsedTransactionRow row : parsedRows) {
            if (!row.isValid()) {
                rejected++;
                warnings.add("Row " + row.getRowNumber() + ": " + row.getErrorMessage());
                continue;
            }

            String fingerprint = generateFingerprint(
                    row.getDate(),
                    row.getDescription(),
                    row.getAmount(),
                    row.getDirection().name(),
                    row.getReferenceNumber()
            );

            // Duplicate detection: check database and current batch
            if (batchFingerprints.contains(fingerprint) || transactionRepository.existsByFingerprint(fingerprint)) {
                duplicates++;
                warnings.add("Row " + row.getRowNumber() + ": Duplicate skipped (" + row.getDate() + ", " + row.getDescription() + ", " + row.getAmount() + ")");
                continue;
            }

            batchFingerprints.add(fingerprint);

            // Categorisation
            CategorizationService.CategoryResult catResult = categorizationService.categorize(
                    row.getDescription(),
                    row.getDirection()
            );

            Transaction tx = new Transaction();
            tx.setImportReference(importRecord.getId());
            tx.setTransactionDate(row.getDate());
            tx.setValueDate(row.getValueDate());
            tx.setDescription(row.getDescription());
            tx.setReferenceNumber(row.getReferenceNumber());
            tx.setAmount(row.getAmount());
            tx.setDirection(row.getDirection());
            tx.setType(catResult.getType());
            tx.setCategory(catResult.getCategory());
            tx.setTransfer(catResult.isTransfer());
            tx.setRunningBalance(row.getBalance());
            tx.setFingerprint(fingerprint);
            tx.setManualCategory(false);
            tx.setRawRowData(row.getRawRow());

            toSave.add(tx);
            accepted++;
        }

        if (!toSave.isEmpty()) {
            transactionRepository.saveAll(toSave);

            for (Transaction t : toSave) {
                if (t.getType() == TransactionType.EXPENSE && !parentControlService.isCategoryAllowed(null, t.getCategory())) {
                    parentControlService.flagImportedRestrictedTransaction(t.getDescription(), t.getAmount(), t.getCategory());
                    warnings.add("Restricted category '" + t.getCategory() + "' detected on imported transaction: " + t.getDescription());
                }
            }
        }

        String status = rejected == parsedRows.size() ? "FAILED" :
                        rejected > 0 || duplicates > 0 ? "PARTIAL" : "SUCCESS";

        importRecord.setStatus(status);
        importRecord.setAcceptedRowCount(accepted);
        importRecord.setDuplicateRowCount(duplicates);
        importRecord.setRejectedRowCount(rejected);
        if (!warnings.isEmpty()) {
            importRecord.setValidationWarnings(String.join("\n", warnings.subList(0, Math.min(warnings.size(), 30))));
        }
        importRepository.save(importRecord);

        BigDecimal inflow = BigDecimal.ZERO;
        BigDecimal outflow = BigDecimal.ZERO;
        LocalDate minDate = null;
        LocalDate maxDate = null;
        Map<String, BigDecimal> catSpend = new HashMap<>();

        for (Transaction t : toSave) {
            if (t.getTransactionDate() != null) {
                if (minDate == null || t.getTransactionDate().isBefore(minDate)) minDate = t.getTransactionDate();
                if (maxDate == null || t.getTransactionDate().isAfter(maxDate)) maxDate = t.getTransactionDate();
            }
            if (t.isTransfer() || t.getType() == com.spendly.entity.TransactionType.TRANSFER) {
                continue;
            }
            if (t.getDirection() == com.spendly.entity.TransactionDirection.CREDIT || t.getType() == com.spendly.entity.TransactionType.INCOME) {
                inflow = inflow.add(t.getAmount());
            } else {
                outflow = outflow.add(t.getAmount());
                String cat = t.getCategory() != null ? t.getCategory() : "Other";
                catSpend.put(cat, catSpend.getOrDefault(cat, BigDecimal.ZERO).add(t.getAmount()));
            }
        }

        String topCat = catSpend.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("General");

        ImportSummaryResponse response = new ImportSummaryResponse();
        response.setImportId(importRecord.getId());
        response.setFilename(filename);
        response.setFileType(extension.toUpperCase());
        response.setStatus(status);
        response.setTotalRows(parsedRows.size());
        response.setAcceptedCount(accepted);
        response.setDuplicateCount(duplicates);
        response.setRejectedCount(rejected);
        response.setValidationWarnings(importRecord.getValidationWarnings());
        response.setImportTimestamp(importRecord.getImportTimestamp());
        response.setTotalInflow(inflow);
        response.setTotalOutflow(outflow);
        response.setStartDate(minDate);
        response.setEndDate(maxDate);
        response.setTopCategory(topCat);
        response.setSampleImportedTransactions(toSave.stream()
                .limit(15)
                .map(TransactionResponse::fromEntity)
                .collect(Collectors.toList()));

        return response;
    }

    public List<StatementImport> getAllImports() {
        return importRepository.findAllByOrderByImportTimestampDesc();
    }

    public StatementImport getImportById(Long id) {
        return importRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Statement import not found with ID: " + id));
    }

    private String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot + 1) : "";
    }

    private String generateFingerprint(Object date, String desc, Object amount, String direction, String ref) {
        try {
            String raw = String.valueOf(date) + "|" +
                         (desc != null ? desc.trim().toLowerCase() : "") + "|" +
                         String.valueOf(amount) + "|" +
                         String.valueOf(direction) + "|" +
                         (ref != null ? ref.trim() : "");
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return UUID.randomUUID().toString().replace("-", "");
        }
    }
}
