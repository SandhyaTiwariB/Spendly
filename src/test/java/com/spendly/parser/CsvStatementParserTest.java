package com.spendly.parser;

import com.spendly.dto.ParsedTransactionRow;
import com.spendly.entity.TransactionDirection;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CsvStatementParserTest {

    private final CsvStatementParser parser = new CsvStatementParser();

    @Test
    void testParseValidCsvWithIndianCurrency() {
        String csv = "Date,Narration,Withdrawal,Deposit,Balance\n" +
                "02/10/2026,Monthly salary,0.00,\"₹40,000.00\",\"40,000.00\"\n" +
                "03/10/2026,Cafe coffee and lunch,420.00,0.00,\"39,580.00\"\n" +
                "04/10/2026,City taxi ride,280.00,0.00,\"39,300.00\"\n" +
                "05/10/2026,Clothing store,\"1,850.00\",0.00,\"37,450.00\"\n" +
                "06/10/2026,Transfer to own savings,\"2,000.00\",0.00,\"35,450.00\"\n";

        List<ParsedTransactionRow> rows = parser.parse(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)), "test.csv");

        assertEquals(5, rows.size());
        assertTrue(rows.stream().allMatch(ParsedTransactionRow::isValid));

        ParsedTransactionRow salary = rows.get(0);
        assertEquals(LocalDate.of(2026, 10, 2), salary.getDate());
        assertEquals("Monthly salary", salary.getDescription());
        assertEquals(new BigDecimal("40000.00"), salary.getAmount());
        assertEquals(TransactionDirection.CREDIT, salary.getDirection());

        ParsedTransactionRow food = rows.get(1);
        assertEquals(new BigDecimal("420.00"), food.getAmount());
        assertEquals(TransactionDirection.DEBIT, food.getDirection());
    }

    @Test
    void testParseEmptyCsvThrowsException() {
        assertThrows(Exception.class, () ->
                parser.parse(new ByteArrayInputStream(new byte[0]), "empty.csv"));
    }

    @Test
    void testParseMalformedRowsReportsInvalidRow() {
        String csv = "Date,Description,Debit,Credit\n" +
                "NOT_A_DATE,Bad row,100,0\n" +
                "02/10/2026,Good row,0,500\n";

        List<ParsedTransactionRow> rows = parser.parse(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)), "malformed.csv");
        assertEquals(2, rows.size());
        assertFalse(rows.get(0).isValid());
        assertTrue(rows.get(1).isValid());
    }
}
