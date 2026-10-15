package com.spendly.parser;

import com.spendly.dto.ParsedTransactionRow;
import com.spendly.entity.TransactionDirection;
import com.spendly.exception.StatementParsingException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

@Component
public class CsvStatementParser implements StatementParser {

    @Override
    public boolean supports(String filename, String contentType) {
        if (filename == null) return false;
        String lower = filename.toLowerCase();
        return lower.endsWith(".csv") || lower.endsWith(".tsv") || lower.endsWith(".txt") ||
                (contentType != null && (contentType.contains("csv") || contentType.contains("text/plain") || contentType.contains("tab-separated")));
    }

    @Override
    public List<ParsedTransactionRow> parse(InputStream inputStream, String filename) {
        List<ParsedTransactionRow> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    lines.add(line);
                }
            }

            if (lines.isEmpty()) {
                throw new StatementParsingException("Statement file is empty.");
            }

            // Auto-detect delimiter and header row index
            int headerIndex = -1;
            char delimiter = ',';

            for (int i = 0; i < Math.min(lines.size(), 60); i++) {
                String candidate = lines.get(i).toLowerCase();
                char delim = detectDelimiter(lines.get(i));
                boolean hasDate = candidate.contains("date") || candidate.contains("txn") || candidate.contains("tran");
                boolean hasDesc = candidate.contains("narration") || candidate.contains("desc") || candidate.contains("particular") ||
                        candidate.contains("detail") || candidate.contains("remark") || candidate.contains("memo") || candidate.contains("merchant") || candidate.contains("party");
                boolean hasAmount = candidate.contains("debit") || candidate.contains("credit") || candidate.contains("amount") ||
                        candidate.contains("withdrawal") || candidate.contains("deposit") || candidate.contains("amt");

                if (hasDate && (hasDesc || hasAmount)) {
                    headerIndex = i;
                    delimiter = delim;
                    break;
                }
            }

            // Fallback 1: check if row 0 has headers
            if (headerIndex == -1) {
                delimiter = detectDelimiter(lines.get(0));
                headerIndex = 0;
            }

            StringBuilder csvContent = new StringBuilder();
            for (int i = headerIndex; i < lines.size(); i++) {
                csvContent.append(lines.get(i)).append("\n");
            }

            CSVFormat format = CSVFormat.DEFAULT.builder()
                    .setDelimiter(delimiter)
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setIgnoreHeaderCase(true)
                    .setTrim(true)
                    .setAllowMissingColumnNames(true)
                    .build();

            try (CSVParser parser = CSVParser.parse(csvContent.toString(), format)) {
                Map<String, Integer> headerMap = parser.getHeaderMap();
                int dateCol = findCol(headerMap, "date", "txn date", "transaction date", "value date", "post date", "posting date", "book date", "tran date", "date of txn");
                int descCol = findCol(headerMap, "narration", "description", "particulars", "remarks", "details", "transaction details", "merchant", "payee", "memo", "notes", "party", "txn desc");
                int debitCol = findCol(headerMap, "debit", "withdrawal", "dr", "withdrawals", "debit amount", "dr amount", "paid out", "money out", "spend");
                int creditCol = findCol(headerMap, "credit", "deposit", "cr", "deposits", "credit amount", "cr amount", "paid in", "money in", "receipt");
                int amountCol = findCol(headerMap, "amount", "transaction amount", "txn amount", "net amount", "amt", "total amount");
                int typeCol = findCol(headerMap, "type", "txn type", "cr/dr", "dr/cr", "d/c", "indicator", "direction", "trans type");
                int balanceCol = findCol(headerMap, "balance", "closing balance", "running balance", "net balance", "available balance", "bal");
                int refCol = findCol(headerMap, "ref", "reference", "utr", "chq", "cheque", "txn id", "transaction id", "reference number", "journal no", "instrument");

                // If column names not found in header map, try positional guessing from first record
                if (dateCol == -1 || (descCol == -1 && amountCol == -1 && debitCol == -1)) {
                    // Try positional heuristic
                    dateCol = 0;
                    descCol = 1;
                    if (headerMap.size() >= 4) {
                        debitCol = 2;
                        creditCol = 3;
                    } else if (headerMap.size() >= 3) {
                        amountCol = 2;
                    }
                }

                int rowNum = headerIndex + 1;
                for (CSVRecord record : parser) {
                    rowNum++;
                    ParsedTransactionRow row = new ParsedTransactionRow();
                    row.setRowNumber(rowNum);
                    row.setRawRow(record.toString());

                    try {
                        String rawDate = getRecordValue(record, dateCol);
                        LocalDate parsedDate = StatementParseUtils.parseDate(rawDate);
                        if (parsedDate == null) {
                            // If first column wasn't a valid date, maybe another column has the date
                            for (int c = 0; c < Math.min(record.size(), 3); c++) {
                                parsedDate = StatementParseUtils.parseDate(getRecordValue(record, c));
                                if (parsedDate != null) {
                                    dateCol = c;
                                    break;
                                }
                            }
                        }

                        if (parsedDate == null) {
                            boolean hasAnyContent = false;
                            for (int c = 0; c < record.size(); c++) {
                                String v = getRecordValue(record, c);
                                if (v != null && !v.trim().isEmpty()) {
                                    hasAnyContent = true;
                                    break;
                                }
                            }
                            if (!hasAnyContent) {
                                continue;
                            }
                            row.setValid(false);
                            row.setErrorMessage("Invalid or missing date: " + rawDate);
                            rows.add(row);
                            continue;
                        }
                        row.setDate(parsedDate);

                        String rawDesc = getRecordValue(record, descCol);
                        row.setDescription(StatementParseUtils.cleanDescription(rawDesc));

                        if (refCol != -1) {
                            String ref = getRecordValue(record, refCol);
                            if (ref != null && !ref.trim().isEmpty()) {
                                row.setReferenceNumber(ref.trim());
                            }
                        }

                        if (balanceCol != -1) {
                            BigDecimal bal = StatementParseUtils.parseAmount(getRecordValue(record, balanceCol));
                            row.setBalance(bal);
                        }

                        BigDecimal debit = debitCol != -1 ? StatementParseUtils.parseAmount(getRecordValue(record, debitCol)) : null;
                        BigDecimal credit = creditCol != -1 ? StatementParseUtils.parseAmount(getRecordValue(record, creditCol)) : null;
                        BigDecimal amount = amountCol != -1 ? StatementParseUtils.parseAmount(getRecordValue(record, amountCol)) : null;
                        String typeVal = typeCol != -1 ? getRecordValue(record, typeCol) : null;

                        row.setDebit(debit);
                        row.setCredit(credit);

                        if (debit != null && debit.compareTo(BigDecimal.ZERO) > 0) {
                            row.setAmount(debit);
                            row.setDirection(TransactionDirection.DEBIT);
                        } else if (credit != null && credit.compareTo(BigDecimal.ZERO) > 0) {
                            row.setAmount(credit);
                            row.setDirection(TransactionDirection.CREDIT);
                        } else if (amount != null) {
                            BigDecimal absAmount = amount.abs();
                            row.setAmount(absAmount);

                            if (typeVal != null) {
                                String t = typeVal.trim().toUpperCase();
                                if (t.startsWith("DR") || t.startsWith("DEBIT") || t.equals("D") || t.startsWith("WDL") || t.startsWith("OUT")) {
                                    row.setDirection(TransactionDirection.DEBIT);
                                } else {
                                    row.setDirection(TransactionDirection.CREDIT);
                                }
                            } else if (amount.compareTo(BigDecimal.ZERO) < 0) {
                                row.setDirection(TransactionDirection.DEBIT);
                            } else {
                                row.setDirection(TransactionDirection.CREDIT);
                            }
                        } else {
                            // If both debit and credit columns were null/empty, check other columns for amounts
                            BigDecimal fallbackAmount = null;
                            for (int c = 2; c < record.size(); c++) {
                                fallbackAmount = StatementParseUtils.parseAmount(getRecordValue(record, c));
                                if (fallbackAmount != null) break;
                            }
                            if (fallbackAmount != null) {
                                row.setAmount(fallbackAmount.abs());
                                row.setDirection(fallbackAmount.compareTo(BigDecimal.ZERO) < 0 ? TransactionDirection.DEBIT : TransactionDirection.CREDIT);
                            } else {
                                continue;
                            }
                        }

                        rows.add(row);
                    } catch (Exception ex) {
                        row.setValid(false);
                        row.setErrorMessage("Row processing notice: " + ex.getMessage());
                    }
                }
            }
        } catch (StatementParsingException spe) {
            throw spe;
        } catch (Exception e) {
            throw new StatementParsingException("Failed to parse CSV statement: " + e.getMessage(), e);
        }

        return rows;
    }

    private char detectDelimiter(String line) {
        if (line == null) return ',';
        int commas = countChars(line, ',');
        int semicolons = countChars(line, ';');
        int tabs = countChars(line, '\t');
        int pipes = countChars(line, '|');

        if (tabs > commas && tabs > semicolons) return '\t';
        if (semicolons > commas && semicolons > pipes) return ';';
        if (pipes > commas && pipes > semicolons) return '|';
        return ',';
    }

    private int countChars(String str, char ch) {
        int count = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == ch) count++;
        }
        return count;
    }

    private int findCol(Map<String, Integer> headerMap, String... candidates) {
        for (String candidate : candidates) {
            for (Map.Entry<String, Integer> entry : headerMap.entrySet()) {
                if (entry.getKey() != null && entry.getKey().trim().equalsIgnoreCase(candidate)) {
                    return entry.getValue();
                }
            }
        }
        // Substring match fallback
        for (String candidate : candidates) {
            for (Map.Entry<String, Integer> entry : headerMap.entrySet()) {
                if (entry.getKey() != null && entry.getKey().toLowerCase().contains(candidate)) {
                    return entry.getValue();
                }
            }
        }
        return -1;
    }

    private String getRecordValue(CSVRecord record, int index) {
        if (index >= 0 && index < record.size()) {
            return record.get(index);
        }
        return null;
    }
}
