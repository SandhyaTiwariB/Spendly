package com.spendly.parser;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Locale;

public class StatementParseUtils {

    private static final DateTimeFormatter[] DATE_FORMATTERS = new DateTimeFormatter[]{
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("dd.MM.yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("yyyy.MM.dd"),
            DateTimeFormatter.ofPattern("dd/MM/yy"),
            DateTimeFormatter.ofPattern("dd-MM-yy"),
            DateTimeFormatter.ofPattern("dd.MM.yy"),
            new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("dd MMM yyyy").toFormatter(Locale.ENGLISH),
            new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("dd-MMM-yyyy").toFormatter(Locale.ENGLISH),
            new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("dd.MMM.yyyy").toFormatter(Locale.ENGLISH),
            new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("dd/MMM/yyyy").toFormatter(Locale.ENGLISH),
            new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("d MMM yyyy").toFormatter(Locale.ENGLISH),
            new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("d-MMM-yyyy").toFormatter(Locale.ENGLISH),
            new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("d.MMM.yyyy").toFormatter(Locale.ENGLISH),
            new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("dd MMM yy").toFormatter(Locale.ENGLISH),
            new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("dd-MMM-yy").toFormatter(Locale.ENGLISH),
            new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("dd.MMM.yy").toFormatter(Locale.ENGLISH),
            new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("d MMM yy").toFormatter(Locale.ENGLISH),
            new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("dd MMMM yyyy").toFormatter(Locale.ENGLISH),
            new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("d MMMM yyyy").toFormatter(Locale.ENGLISH),
            new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("MMM dd, yyyy").toFormatter(Locale.ENGLISH),
            new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("MMM d, yyyy").toFormatter(Locale.ENGLISH),
            new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("MMMM dd, yyyy").toFormatter(Locale.ENGLISH),
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("d-M-yyyy"),
            DateTimeFormatter.ofPattern("d.M.yyyy"),
            DateTimeFormatter.ofPattern("d/M/yy"),
            DateTimeFormatter.ofPattern("d-M-yy"),
            DateTimeFormatter.ofPattern("d.M.yy"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("MM-dd-yyyy"),
            DateTimeFormatter.ofPattern("M/d/yyyy"),
            DateTimeFormatter.ofPattern("M-d-yyyy")
    };

    public static LocalDate parseDate(String raw) {
        if (raw == null) return null;
        String clean = raw.trim();
        if (clean.isEmpty()) return null;

        // If contains 'T' (ISO format like 2026-10-02T14:30:00)
        if (clean.contains("T")) {
            clean = clean.substring(0, clean.indexOf("T")).trim();
        }

        // Strip time if present: e.g. "2026-10-02 14:30:00" -> "2026-10-02"
        if (clean.contains(" ")) {
            String[] parts = clean.split("\\s+");
            if (parts.length > 0 && (parts[0].contains("-") || parts[0].contains("/") || parts[0].contains("."))) {
                clean = parts[0];
            } else if (parts.length >= 3 && parts[1].length() == 3) {
                // "02 Oct 2026 14:30:00" -> "02 Oct 2026"
                clean = parts[0] + " " + parts[1] + " " + parts[2];
            }
        }

        // Clean trailing commas, semicolons
        clean = clean.replaceAll("[,;]+$", "").trim();

        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(clean, formatter);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    public static BigDecimal parseAmount(String raw) {
        if (raw == null) return null;
        String clean = raw.trim()
                .replace("\u00A0", "")
                .replace("\u200B", "")
                .trim();
        if (clean.isEmpty() || clean.equals("-") || clean.equalsIgnoreCase("nil") ||
            clean.equalsIgnoreCase("na") || clean.equalsIgnoreCase("n/a") || clean.equalsIgnoreCase("none")) {
            return null;
        }

        boolean negative = false;
        if (clean.startsWith("(") && clean.endsWith(")")) {
            negative = true;
            clean = clean.substring(1, clean.length() - 1).trim();
        } else if (clean.startsWith("-")) {
            negative = true;
            clean = clean.substring(1).trim();
        } else if (clean.endsWith("-")) {
            negative = true;
            clean = clean.substring(0, clean.length() - 1).trim();
        }

        if (clean.endsWith("Dr") || clean.endsWith("DR") || clean.endsWith("dr")) {
            clean = clean.substring(0, clean.length() - 2).trim();
        } else if (clean.endsWith("Cr") || clean.endsWith("CR") || clean.endsWith("cr")) {
            clean = clean.substring(0, clean.length() - 2).trim();
        }

        if (clean.startsWith("(") && clean.endsWith(")")) {
            clean = clean.substring(1, clean.length() - 1).trim();
        }

        // Remove any common currency symbols and labels
        clean = clean.replace("₹", "")
                .replace("Rs.", "")
                .replace("Rs", "")
                .replace("INR", "")
                .replace("$", "")
                .replace("USD", "")
                .replace("€", "")
                .replace("EUR", "")
                .replace("£", "")
                .replace("GBP", "")
                .replace("AED", "")
                .replace("CAD", "")
                .replace("AUD", "")
                .replace("SGD", "")
                .replace("¥", "")
                .trim();

        if (clean.isEmpty()) return null;

        // European format: 1.234,56 -> 1234.56
        if (clean.contains(",") && clean.contains(".")) {
            if (clean.lastIndexOf(',') > clean.lastIndexOf('.')) {
                clean = clean.replace(".", "").replace(',', '.');
            } else {
                clean = clean.replace(",", "");
            }
        } else if (clean.contains(",") && !clean.contains(".")) {
            // Check if comma is decimal (e.g. "123,45") or thousands (e.g. "1,000" or "40,000")
            int commaPos = clean.lastIndexOf(',');
            if (clean.length() - 1 - commaPos == 2) {
                // E.g. "123,45" -> "123.45"
                clean = clean.replace(',', '.');
            } else {
                clean = clean.replace(",", "");
            }
        }

        // Strip any remaining spaces
        clean = clean.replaceAll("\\s+", "");

        try {
            BigDecimal val = new BigDecimal(clean).setScale(2, RoundingMode.HALF_UP);
            return negative ? val.negate() : val;
        } catch (Exception e) {
            return null;
        }
    }

    public static String cleanDescription(String raw) {
        if (raw == null) return "Transaction";
        String cleaned = raw.replaceAll("\\s+", " ").trim();
        return cleaned.isEmpty() ? "Transaction" : cleaned;
    }
}
