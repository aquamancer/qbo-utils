package com.aquamancer.invoicematcher;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class CsvMerger {
    private static final Logger LOGGER = LogManager.getLogger(CsvMerger.class);

    /**
     * Aggregates and returns all rows in each CSV file, given by csvPaths, that satisfy the Predicate filter.
     * @param filePaths List of file paths of CSV files to aggregate.
     * @param filter Predicate CSVRecord that determines whether that CSVRecord should be included.
     * @param csvFormat csvFormat that must include a header mapping using CSVFormat.Builder.create().setHeader().build().
     * @return A List of CSVRecords (rows), aggregated from all CSV files specified by csvPaths, that satisfy the predicate.
    **/
    public static List<CSVRecord> merge(List<String> filePaths, Predicate<? super CSVRecord> filter, CSVFormat csvFormat) {
        csvFormat.builder().setSkipHeaderRecord(true);
        List<CSVRecord> merged = new ArrayList<>();
        FileReader reader;

        LOGGER.info("Merging {} files.", filePaths.size());
        for (String path : filePaths) {
            try {
                LOGGER.debug("Filtering and merging contents of {}", path);
                reader = new FileReader(path);
                merged.addAll(CSVParser.parse(reader, csvFormat)
                        .stream()
                        .filter(record -> filter.test(record) && record.isConsistent())
                        .toList());
            } catch (IOException ex) {
                LOGGER.error("Failed to read file: {}. Excluding from merge.", path);
            }
        }
        LOGGER.info("Merge resulted in a total of: {} CSVRecords.", merged.size());
        return merged;
    }
}
