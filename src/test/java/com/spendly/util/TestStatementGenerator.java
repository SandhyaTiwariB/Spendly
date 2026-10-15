package com.spendly.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;

public class TestStatementGenerator {

    public static void main(String[] args) {
        try {
            File xlsx = new File("sample_statements/sample_statement.xlsx");
            generateExcel(xlsx);
            System.out.println("Generated: " + xlsx.getAbsolutePath());

            File pdf = new File("sample_statements/sample_statement.pdf");
            generatePdf(pdf);
            System.out.println("Generated: " + pdf.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void generateExcel(File targetFile) throws Exception {
        targetFile.getParentFile().mkdirs();
        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(targetFile)) {
            Sheet sheet = workbook.createSheet("Account Statement");

            // Header row
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Date");
            header.createCell(1).setCellValue("Narration");
            header.createCell(2).setCellValue("Debit");
            header.createCell(3).setCellValue("Credit");
            header.createCell(4).setCellValue("Balance");
            header.createCell(5).setCellValue("Reference");

            String[][] rows = new String[][]{
                    {"02/10/2026", "Monthly salary", "0.00", "40000.00", "40000.00", "SAL1001"},
                    {"03/10/2026", "Cafe coffee and lunch", "420.00", "0.00", "39580.00", "SWIGGY1002"},
                    {"04/10/2026", "City taxi ride", "280.00", "0.00", "39300.00", "UBER1003"},
                    {"05/10/2026", "Clothing store", "1850.00", "0.00", "37450.00", "ZARA1004"},
                    {"06/10/2026", "Transfer to own savings", "2000.00", "0.00", "35450.00", "TRF1005"}
            };

            for (int i = 0; i < rows.length; i++) {
                Row row = sheet.createRow(i + 1);
                for (int c = 0; c < rows[i].length; c++) {
                    row.createCell(c).setCellValue(rows[i][c]);
                }
            }

            workbook.write(fos);
        }
    }

    public static void generatePdf(File targetFile) throws Exception {
        targetFile.getParentFile().mkdirs();
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 14);
                stream.setLeading(20f);
                stream.newLineAtOffset(50, 750);
                stream.showText("SPENDLY BANK STATEMENT");
                stream.newLine();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                stream.showText("Account: 1234567890 | Customer: John Doe | Period: Oct 2026");
                stream.newLine();
                stream.newLine();

                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 10);
                stream.showText("Date          Description                       Debit        Credit       Balance");
                stream.newLine();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                stream.showText("02/10/2026    Monthly salary                    0.00         40000.00 Cr  40000.00");
                stream.newLine();
                stream.showText("03/10/2026    Cafe coffee and lunch             420.00 Dr    0.00         39580.00");
                stream.newLine();
                stream.showText("04/10/2026    City taxi ride                    280.00 Dr    0.00         39300.00");
                stream.newLine();
                stream.showText("05/10/2026    Clothing store                    1850.00 Dr   0.00         37450.00");
                stream.newLine();
                stream.showText("06/10/2026    Transfer to own savings           2000.00 Dr   0.00         35450.00");
                stream.endText();
            }

            document.save(targetFile);
        }
    }
}
