package com.spendly.parser;

import com.spendly.dto.ParsedTransactionRow;
import com.spendly.entity.TransactionDirection;
import com.spendly.exception.StatementParsingException;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Component
public class ExcelStatementParser implements StatementParser {

    @Override
    public boolean supports(String filename, String contentType) {
        if (filename == null) return false;
        String lower = filename.toLowerCase();
        return lower.endsWith(".xlsx") || lower.endsWith(".xls") ||
                (contentType != null && (contentType.contains("spreadsheet") || contentType.contains("excel") || contentType.contains("ms-excel")));
    }

    @Override
    public List<ParsedTransactionRow> parse(InputStream inputStream, String filename) {
        List<ParsedTransactionRow> rows = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new StatementParsingException("Excel workbook contains no sheets.");
            }

            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();

            int headerRowIndex = -1;
            int dateCol = -1, descCol = -1, debitCol = -1, creditCol = -1, amountCol = -1, typeCol = -1, balanceCol = -1, refCol = -1;

            for (int r = 0; r <= Math.min(sheet.getLastRowNum(), 60); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                Map<String, Integer> rowHeaders = new HashMap<>();
                for (int c = 0; c < row.getLastCellNum(); c++) {
                    Cell cell = row.getCell(c);
                    String text = formatter.formatCellValue(cell).trim().toLowerCase();
                    if (!text.isEmpty()) {
                        rowHeaders.put(text, c);
                    }
                }

                int candidateDate = findCol(rowHeaders, "date", "txn date", "transaction date", "value date", "post date", "posting date", "book date", "tran date");
                int candidateDesc = findCol(rowHeaders, "narration", "description", "particulars", "remarks", "details", "transaction details", "merchant", "payee", "memo", "notes", "party");
                int candidateDebit = findCol(rowHeaders, "debit", "withdrawal", "dr", "withdrawals", "debit amount", "dr amount", "paid out", "money out");
                int candidateCredit = findCol(rowHeaders, "credit", "deposit", "cr", "deposits", "credit amount", "cr amount", "paid in", "money in");
                int candidateAmount = findCol(rowHeaders, "amount", "transaction amount", "txn amount", "net amount", "amt");

                if (candidateDate != -1 && (candidateDesc != -1 || candidateAmount != -1 || candidateDebit != -1)) {
                    headerRowIndex = r;
                    dateCol = candidateDate;
                    descCol = candidateDesc != -1 ? candidateDesc : 1;
                    debitCol = candidateDebit;
                    creditCol = candidateCredit;
                    amountCol = candidateAmount;
                    typeCol = findCol(rowHeaders, "type", "txn type", "cr/dr", "dr/cr", "d/c", "indicator", "direction");
                    balanceCol = findCol(rowHeaders, "balance", "closing balance", "running balance", "net balance", "available balance", "bal");
                    refCol = findCol(rowHeaders, "ref", "reference", "utr", "chq", "cheque", "txn id", "transaction id", "reference number", "journal no");
                    break;
                }
            }

            if (headerRowIndex == -1) {
                // Heuristic: start at row 0 or 1
                headerRowIndex = 0;
                dateCol = 0;
                descCol = 1;
                debitCol = 2;
                creditCol = 3;
            }

            for (int r = headerRowIndex + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                ParsedTransactionRow parsedRow = new ParsedTransactionRow();
                parsedRow.setRowNumber(r + 1);

                StringBuilder rawBuilder = new StringBuilder();
                for (int c = 0; c < row.getLastCellNum(); c++) {
                    rawBuilder.append(formatter.formatCellValue(row.getCell(c))).append(" | ");
                }
                parsedRow.setRawRow(rawBuilder.toString());

                try {
                    Cell dateCell = row.getCell(dateCol);
                    LocalDate parsedDate = null;
                    if (dateCell != null && dateCell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(dateCell)) {
                        Date d = dateCell.getDateCellValue();
                        parsedDate = d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                    } else if (dateCell != null) {
                        String rawDateStr = formatter.formatCellValue(dateCell);
                        parsedDate = StatementParseUtils.parseDate(rawDateStr);
                    }

                    if (parsedDate == null) {
                        // Check if any other cell in the first 3 columns contains a date
                        for (int c = 0; c < Math.min(row.getLastCellNum(), 3); c++) {
                            Cell altCell = row.getCell(c);
                            if (altCell != null && altCell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(altCell)) {
                                parsedDate = altCell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                                dateCol = c;
                                break;
                            }
                            String testStr = formatter.formatCellValue(altCell);
                            LocalDate testDate = StatementParseUtils.parseDate(testStr);
                            if (testDate != null) {
                                parsedDate = testDate;
                                dateCol = c;
                                break;
                            }
                        }
                    }

                    if (parsedDate == null) {
                        boolean hasAny = false;
                        for (int c = 0; c < row.getLastCellNum(); c++) {
                            Cell cell = row.getCell(c);
                            if (cell != null && !formatter.formatCellValue(cell).trim().isEmpty()) {
                                hasAny = true;
                                break;
                            }
                        }
                        if (!hasAny) {
                            continue;
                        }
                        parsedRow.setValid(false);
                        parsedRow.setErrorMessage("Invalid or missing date");
                        rows.add(parsedRow);
                        continue;
                    }
                    parsedRow.setDate(parsedDate);

                    String rawDesc = descCol != -1 && row.getCell(descCol) != null ? formatter.formatCellValue(row.getCell(descCol)) : "Transaction";
                    parsedRow.setDescription(StatementParseUtils.cleanDescription(rawDesc));

                    if (refCol != -1 && row.getCell(refCol) != null) {
                        String ref = formatter.formatCellValue(row.getCell(refCol)).trim();
                        if (!ref.isEmpty()) parsedRow.setReferenceNumber(ref);
                    }

                    if (balanceCol != -1 && row.getCell(balanceCol) != null) {
                        BigDecimal bal = StatementParseUtils.parseAmount(formatter.formatCellValue(row.getCell(balanceCol)));
                        parsedRow.setBalance(bal);
                    }

                    BigDecimal debit = debitCol != -1 && row.getCell(debitCol) != null ? StatementParseUtils.parseAmount(formatter.formatCellValue(row.getCell(debitCol))) : null;
                    BigDecimal credit = creditCol != -1 && row.getCell(creditCol) != null ? StatementParseUtils.parseAmount(formatter.formatCellValue(row.getCell(creditCol))) : null;
                    BigDecimal amount = amountCol != -1 && row.getCell(amountCol) != null ? StatementParseUtils.parseAmount(formatter.formatCellValue(row.getCell(amountCol))) : null;
                    String typeVal = typeCol != -1 && row.getCell(typeCol) != null ? formatter.formatCellValue(row.getCell(typeCol)) : null;

                    parsedRow.setDebit(debit);
                    parsedRow.setCredit(credit);

                    if (debit != null && debit.compareTo(BigDecimal.ZERO) > 0) {
                        parsedRow.setAmount(debit);
                        parsedRow.setDirection(TransactionDirection.DEBIT);
                    } else if (credit != null && credit.compareTo(BigDecimal.ZERO) > 0) {
                        parsedRow.setAmount(credit);
                        parsedRow.setDirection(TransactionDirection.CREDIT);
                    } else if (amount != null) {
                        parsedRow.setAmount(amount.abs());
                        if (typeVal != null) {
                            String t = typeVal.trim().toUpperCase();
                            if (t.startsWith("DR") || t.startsWith("DEBIT") || t.equals("D") || t.startsWith("WDL") || t.startsWith("OUT")) {
                                parsedRow.setDirection(TransactionDirection.DEBIT);
                            } else {
                                parsedRow.setDirection(TransactionDirection.CREDIT);
                            }
                        } else if (amount.compareTo(BigDecimal.ZERO) < 0) {
                            parsedRow.setDirection(TransactionDirection.DEBIT);
                        } else {
                            parsedRow.setDirection(TransactionDirection.CREDIT);
                        }
                    } else {
                        continue;
                    }

                    rows.add(parsedRow);
                } catch (Exception ex) {
                    parsedRow.setValid(false);
                    parsedRow.setErrorMessage("Row error: " + ex.getMessage());
                }
            }
        } catch (StatementParsingException spe) {
            throw spe;
        } catch (Exception e) {
            throw new StatementParsingException("Failed to parse Excel statement: " + e.getMessage(), e);
        }

        return rows;
    }

    private int findCol(Map<String, Integer> headerMap, String... candidates) {
        for (String candidate : candidates) {
            for (Map.Entry<String, Integer> entry : headerMap.entrySet()) {
                if (entry.getKey() != null && entry.getKey().trim().equalsIgnoreCase(candidate)) {
                    return entry.getValue();
                }
            }
        }
        for (String candidate : candidates) {
            for (Map.Entry<String, Integer> entry : headerMap.entrySet()) {
                if (entry.getKey() != null && entry.getKey().toLowerCase().contains(candidate)) {
                    return entry.getValue();
                }
            }
        }
        return -1;
    }
}
