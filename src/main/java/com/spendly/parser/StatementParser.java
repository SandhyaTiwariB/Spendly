package com.spendly.parser;

import com.spendly.dto.ParsedTransactionRow;
import java.io.InputStream;
import java.util.List;

public interface StatementParser {
    boolean supports(String filename, String contentType);
    List<ParsedTransactionRow> parse(InputStream inputStream, String filename);
}
