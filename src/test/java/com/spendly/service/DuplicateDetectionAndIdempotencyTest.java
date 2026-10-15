package com.spendly.service;

import com.spendly.dto.ImportSummaryResponse;
import com.spendly.parser.CsvStatementParser;
import com.spendly.parser.StatementParserFactory;
import com.spendly.repository.CategoryRepository;
import com.spendly.repository.StatementImportRepository;
import com.spendly.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DuplicateDetectionAndIdempotencyTest {

    @Autowired
    private StatementImportService importService;

    @Autowired
    private TransactionRepository transactionRepository;

    @Test
    void testReimportingSameStatementIsIdempotent() {
        String csv = "Date,Narration,Withdrawal,Deposit,Balance\n" +
                "02/10/2026,Monthly salary,0.00,40000.00,40000.00\n" +
                "03/10/2026,Cafe coffee and lunch,420.00,0.00,39580.00\n" +
                "04/10/2026,City taxi ride,280.00,0.00,39300.00\n" +
                "05/10/2026,Clothing store,1850.00,0.00,37450.00\n" +
                "06/10/2026,Transfer to own savings,2000.00,0.00,35450.00\n";

        MockMultipartFile file1 = new MockMultipartFile(
                "file", "statement.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8)
        );

        ImportSummaryResponse firstImport = importService.importStatement(file1);
        assertEquals(5, firstImport.getAcceptedCount());
        assertEquals(0, firstImport.getDuplicateCount());
        assertEquals(5, transactionRepository.count());

        // Re-importing exact same statement
        MockMultipartFile file2 = new MockMultipartFile(
                "file", "statement.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8)
        );

        ImportSummaryResponse secondImport = importService.importStatement(file2);
        assertEquals(0, secondImport.getAcceptedCount());
        assertEquals(5, secondImport.getDuplicateCount());
        assertEquals(5, transactionRepository.count(), "Transaction count must remain 5 and not duplicate!");
    }
}
