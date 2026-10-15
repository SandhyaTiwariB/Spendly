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

class ExcelStatementParserTest {

    private final ExcelStatementParser parser = new ExcelStatementParser();

    @Test
    void testParseExcelStatement(@TempDir Path tempDir) throws Exception {
        File file = tempDir.resolve("sample.xlsx").toFile();
        TestStatementGenerator.generateExcel(file);

        List<ParsedTransactionRow> rows;
        try (FileInputStream fis = new FileInputStream(file)) {
            rows = parser.parse(fis, file.getName());
        }

        assertEquals(5, rows.size());
        assertTrue(rows.stream().allMatch(ParsedTransactionRow::isValid));

        ParsedTransactionRow salary = rows.get(0);
        assertEquals("Monthly salary", salary.getDescription());
        assertEquals(new BigDecimal("40000.00"), salary.getAmount());
        assertEquals(TransactionDirection.CREDIT, salary.getDirection());

        ParsedTransactionRow taxi = rows.get(2);
        assertEquals("City taxi ride", taxi.getDescription());
        assertEquals(new BigDecimal("280.00"), taxi.getAmount());
        assertEquals(TransactionDirection.DEBIT, taxi.getDirection());
    }
}
