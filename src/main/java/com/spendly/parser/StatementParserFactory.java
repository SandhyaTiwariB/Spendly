package com.spendly.parser;

import com.spendly.exception.InvalidFileException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StatementParserFactory {

    private final List<StatementParser> parsers;

    public StatementParserFactory(List<StatementParser> parsers) {
        this.parsers = parsers;
    }

    public StatementParser getParser(String filename, String contentType) {
        for (StatementParser parser : parsers) {
            if (parser.supports(filename, contentType)) {
                return parser;
            }
        }
        throw new InvalidFileException("Unsupported statement format for file: " + filename + ". Supported formats are CSV, Excel (.xls, .xlsx) and text-based PDF.");
    }
}
