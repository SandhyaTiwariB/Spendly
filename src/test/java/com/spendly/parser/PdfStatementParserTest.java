package com.spendly.parser;

import com.spendly.dto.ParsedTransactionRow;
import com.spendly.entity.TransactionDirection;
import com.spendly.util.TestStatementGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PdfStatementParserTest {

    private final PdfStatementParser parser = new PdfStatementParser();

    @Test
    void testParsePdfStatement(@TempDir Path tempDir) throws Exception {
        File file = tempDir.resolve("sample.pdf").toFile();
        TestStatementGenerator.generatePdf(file);

        List<ParsedTransactionRow> rows;
        try (FileInputStream fis = new FileInputStream(file)) {
            rows = parser.parse(fis, file.getName());
        }

        assertEquals(5, rows.size());
        assertTrue(rows.stream().allMatch(ParsedTransactionRow::isValid));

        ParsedTransactionRow salary = rows.get(0);
        assertTrue(salary.getDescription().toLowerCase().contains("salary"));
        assertEquals(new BigDecimal("40000.00"), salary.getAmount());
        assertEquals(TransactionDirection.CREDIT, salary.getDirection());

        ParsedTransactionRow food = rows.get(1);
        assertTrue(food.getDescription().toLowerCase().contains("cafe"));
        assertEquals(new BigDecimal("420.00"), food.getAmount());
        assertEquals(TransactionDirection.DEBIT, food.getDirection());
    }
}
