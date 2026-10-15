package com.spendly.util;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertTrue;

class StatementGeneratorTest {

    @Test
    void generateSampleStatements() throws Exception {
        File xlsx = new File("sample_statements/sample_statement.xlsx");
        TestStatementGenerator.generateExcel(xlsx);
        assertTrue(xlsx.exists());
        assertTrue(xlsx.length() > 0);

        File pdf = new File("sample_statements/sample_statement.pdf");
        TestStatementGenerator.generatePdf(pdf);
        assertTrue(pdf.exists());
        assertTrue(pdf.length() > 0);
    }
}
