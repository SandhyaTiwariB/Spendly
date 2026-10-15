package com.spendly.parser;

import com.spendly.dto.ParsedTransactionRow;
import com.spendly.entity.TransactionDirection;
import com.spendly.exception.StatementParsingException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class PdfStatementParser implements StatementParser {

    // Regex to locate a date anywhere near the start of a line (token 0, 1, or 2)
    // Matches dd/MM/yyyy, dd-MM-yyyy, dd.MM.yyyy, yyyy-MM-dd, dd MMM yyyy, dd-MMM-yyyy, dd.MMM.yyyy, etc.
    private static final Pattern INLINE_DATE_PATTERN = Pattern.compile(
            "\\b(\\d{1,2}[/.-]\\d{1,2}[/.-]\\d{2,4}|\\d{4}[/.-]\\d{1,2}[/.-]\\d{1,2}|\\d{1,2}\\s+[A-Za-z]{3,9}\\s+\\d{2,4}|\\d{1,2}[/-][A-Za-z]{3,9}[/-]\\d{2,4})\\b"
    );

    @Override
    public boolean supports(String filename, String contentType) {
        if (filename == null) return false;
        String lower = filename.toLowerCase();
        return lower.endsWith(".pdf") || (contentType != null && contentType.contains("pdf"));
    }

    @Override
    public List<ParsedTransactionRow> parse(InputStream inputStream, String filename) {
        List<ParsedTransactionRow> rows = new ArrayList<>();
        try {
            byte[] bytes = inputStream.readAllBytes();
            if (bytes.length == 0) {
                throw new StatementParsingException("PDF statement file is empty.");
            }

            String fullText;
            try (PDDocument document = Loader.loadPDF(bytes)) {
                if (document.isEncrypted()) {
                    throw new StatementParsingException(
                            "This PDF statement is password-protected or encrypted. Please remove password protection before uploading."
                    );
                }
                if (document.getNumberOfPages() == 0) {
                    throw new StatementParsingException("PDF document contains no pages.");
                }
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setSortByPosition(true);
                fullText = stripper.getText(document);
            }

            if (fullText == null || fullText.trim().length() < 30) {
                throw new StatementParsingException(
                        "Text extraction was unsuccessful. This PDF appears to be a scanned image-only statement without an embedded text layer. " +
                        "Please upload a digital text-based PDF statement, CSV, or Excel file."
                );
            }

            String[] rawLines = fullText.split("\\r?\\n");
            int lineNum = 0;
            ParsedTransactionRow currentRow = null;

            for (String line : rawLines) {
                lineNum++;
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;

                // Skip obvious non-transaction headers and footers
                if (isHeaderOrFooter(trimmed)) {
                    continue;
                }

                // Check for a date pattern in this line
                Matcher dateMatcher = INLINE_DATE_PATTERN.matcher(trimmed);
                if (dateMatcher.find() && dateMatcher.start() <= 25) { // Date is near start of line
                    String dateStr = dateMatcher.group(1);
                    LocalDate txDate = StatementParseUtils.parseDate(dateStr);

                    if (txDate != null) {
                        // We found a new transaction row start!
                        currentRow = new ParsedTransactionRow();
                        currentRow.setRowNumber(lineNum);
                        currentRow.setRawRow(trimmed);
                        currentRow.setDate(txDate);

                        // Strip date from the line to process the rest
                        String beforeDate = trimmed.substring(0, dateMatcher.start()).trim();
                        String afterDate = trimmed.substring(dateMatcher.end()).trim();
                        String content = (beforeDate.isEmpty() ? "" : beforeDate + " ") + afterDate;

                        boolean parsedOk = parseRowContent(currentRow, content);
                        if (parsedOk) {
                            rows.add(currentRow);
                        } else {
                            // Keep row to try accumulating next line
                            rows.add(currentRow);
                        }
                        continue;
                    }
                }

                // If no date found, but we have an existing row, this might be a multiline continuation
                if (currentRow != null) {
                    // Check if line contains amounts that were missing from previous line
                    if (currentRow.getAmount() == null) {
                        parseRowContent(currentRow, trimmed);
                    } else {
                        // Append continuation text to description if not a metadata line
                        if (!trimmed.toLowerCase().startsWith("page ") && !trimmed.toLowerCase().startsWith("total ")) {
                            String updatedDesc = currentRow.getDescription() + " " + trimmed;
                            currentRow.setDescription(StatementParseUtils.cleanDescription(updatedDesc));
                        }
                    }
                }
            }

            // Filter out any rows that could not resolve an amount
            rows.removeIf(r -> r.getAmount() == null || r.getAmount().compareTo(BigDecimal.ZERO) <= 0);

            if (rows.isEmpty()) {
                throw new StatementParsingException(
                        "No structured transaction records could be extracted from this PDF. " +
                        "Please ensure the file is an account statement containing dates, narrations, and transaction amounts."
                );
            }

        } catch (StatementParsingException spe) {
            throw spe;
        } catch (Exception e) {
            throw new StatementParsingException("Failed to parse PDF bank statement: " + e.getMessage(), e);
        }

        return rows;
    }

    private boolean isHeaderOrFooter(String line) {
        String lower = line.toLowerCase();
        return lower.startsWith("page ") ||
               lower.contains("page number") ||
               lower.contains("generated on") ||
               lower.contains("statement of account") ||
               lower.contains("account summary") ||
               lower.contains("customer id") ||
               lower.contains("account number") ||
               lower.contains("micr code") ||
               lower.contains("ifsc code") ||
               lower.contains("branch address") ||
               lower.contains("nomination registered") ||
               lower.contains("opening balance") ||
               lower.contains("closing balance") ||
               lower.startsWith("total withdrawal") ||
               lower.startsWith("total deposit");
    }

    private boolean parseRowContent(ParsedTransactionRow row, String text) {
        try {
            String[] tokens = text.split("\\s+");
            List<String> textParts = new ArrayList<>();
            List<BigDecimal> amounts = new ArrayList<>();

            boolean hasDr = false;
            boolean hasCr = false;

            String lower = text.toLowerCase();
            if (lower.contains(" dr") || lower.contains(" debit") || lower.endsWith("dr") || lower.contains("(dr)")) {
                hasDr = true;
            }
            if (lower.contains(" cr") || lower.contains(" credit") || lower.endsWith("cr") || lower.contains("(cr)")) {
                hasCr = true;
            }

            for (String token : tokens) {
                String cleanToken = token.trim();
                if (cleanToken.equalsIgnoreCase("dr") || cleanToken.equalsIgnoreCase("cr") ||
                    cleanToken.equalsIgnoreCase("debit") || cleanToken.equalsIgnoreCase("credit")) {
                    continue;
                }

                // Check if this token is a valid amount
                BigDecimal amt = StatementParseUtils.parseAmount(cleanToken);
                if (amt != null && amt.compareTo(BigDecimal.ZERO) >= 0 && containsDigit(cleanToken)) {
                    // Check if it's not a reference number (usually refs don't have decimals or are long integers)
                    if (cleanToken.contains(".") || amt.compareTo(new BigDecimal("100000000")) < 0) {
                        amounts.add(amt);
                        continue;
                    }
                }
                textParts.add(cleanToken);
            }

            if (!textParts.isEmpty() && (row.getDescription() == null || row.getDescription().equals("Transaction"))) {
                row.setDescription(StatementParseUtils.cleanDescription(String.join(" ", textParts)));
            }

            if (amounts.isEmpty()) {
                return false;
            }

            // Determine Debit / Credit / Balance from amounts
            if (amounts.size() == 1) {
                BigDecimal single = amounts.get(0);
                row.setAmount(single);
                if (hasDr) {
                    row.setDirection(TransactionDirection.DEBIT);
                } else if (hasCr) {
                    row.setDirection(TransactionDirection.CREDIT);
                } else {
                    // Heuristic: check description keywords
                    row.setDirection(guessDirectionFromDesc(row.getDescription()));
                }
                return true;
            } else if (amounts.size() == 2) {
                // Usually [Amount, RunningBalance]
                BigDecimal txAmount = amounts.get(0);
                BigDecimal balance = amounts.get(1);
                row.setAmount(txAmount);
                row.setBalance(balance);

                if (hasDr) {
                    row.setDirection(TransactionDirection.DEBIT);
                } else if (hasCr) {
                    row.setDirection(TransactionDirection.CREDIT);
                } else {
                    row.setDirection(guessDirectionFromDesc(row.getDescription()));
                }
                return true;
            } else {
                // amounts.size() >= 3: Typically [Debit, Credit, Balance] or [Withdrawal, Deposit, Balance]
                BigDecimal val1 = amounts.get(0);
                BigDecimal val2 = amounts.get(1);
                BigDecimal balance = amounts.get(amounts.size() - 1);
                row.setBalance(balance);

                if (val1.compareTo(BigDecimal.ZERO) > 0 && val2.compareTo(BigDecimal.ZERO) == 0) {
                    row.setAmount(val1);
                    row.setDirection(TransactionDirection.DEBIT);
                } else if (val2.compareTo(BigDecimal.ZERO) > 0 && val1.compareTo(BigDecimal.ZERO) == 0) {
                    row.setAmount(val2);
                    row.setDirection(TransactionDirection.CREDIT);
                } else if (val1.compareTo(BigDecimal.ZERO) > 0) {
                    row.setAmount(val1);
                    row.setDirection(hasDr ? TransactionDirection.DEBIT : (hasCr ? TransactionDirection.CREDIT : guessDirectionFromDesc(row.getDescription())));
                }
                return true;
            }
        } catch (Exception e) {
            return false;
        }
    }

    private boolean containsDigit(String s) {
        for (char c : s.toCharArray()) {
            if (Character.isDigit(c)) return true;
        }
        return false;
    }

    private TransactionDirection guessDirectionFromDesc(String desc) {
        if (desc == null) return TransactionDirection.DEBIT;
        String lower = desc.toLowerCase();
        if (lower.contains("salary") || lower.contains("interest") || lower.contains("dividend") ||
            lower.contains("refund") || lower.contains("credit") || lower.contains("deposit") ||
            lower.contains("cashback") || lower.contains("received") || lower.contains("reversal")) {
            return TransactionDirection.CREDIT;
        }
        return TransactionDirection.DEBIT;
    }
}
