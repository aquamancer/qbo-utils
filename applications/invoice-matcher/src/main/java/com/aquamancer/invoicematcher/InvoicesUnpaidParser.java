package com.aquamancer.invoicematcher;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class InvoicesUnpaidParser {
    private static final Logger LOGGER = LogManager.getLogger(InvoicesUnpaidParser.class);
    public static final CSVFormat invoicesUnpaidFormat = CSVFormat.Builder.create().setHeader(
            "Invoice",
            "Date",
            "Num",
            "Posting",
            "Name",
            "Memo/Description",
            "Account full name",
            "Amount",
            "Open balance"
    ).build();
    public static Map<String, List<CSVRecord>> invoicesUnpaidToMap(FileReader invoicesUnpaidCsv) {
        try {
            List<CSVRecord> invoicesUnpaid = CSVParser.parse(invoicesUnpaidCsv, invoicesUnpaidFormat).getRecords();
            return invoicesUnpaid.stream()
                    .filter(record -> {
                        String invoiceNumber = record.get(Headers.INVOICES_UNPAID.get("invoiceNumber"));
                        return !invoiceNumber.isEmpty() && !invoiceNumber.equals("Num"); // todo hardcoded "Num"
                    })
                    .collect(Collectors.groupingBy(record -> record.get(Headers.INVOICES_UNPAID.get("invoiceNumber"))));
        } catch (IOException ex) {
            LOGGER.fatal("Failed parsing invoices unpaid.");
            throw new RuntimeException("Could not parse invoices unpaid!");
        }
    }
}
