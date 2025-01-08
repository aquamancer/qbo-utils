package com.aquamancer.invoicematcher;
import org.apache.commons.csv.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.format.DateTimeFormatter;

public final class BankDepositParser {
    private static final Logger LOGGER = LogManager.getLogger(BankDepositParser.class);
    public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    public static double parseAmount(CSVRecord deposit, String header) {
        String amount = deposit.get(header).replaceAll(",", "");
        if (amount.indexOf('$') == 0) {
            return Double.parseDouble(amount.substring(1));
        } else {
            LOGGER.error("Bank deposit parser was used to parse: {}, but doesn't have a '$' at index 0.", deposit.get(header));
            return Double.parseDouble(amount);
        }
    }
    /**
     * Private constructor to prevent instantiation.
     */
    private BankDepositParser() {}
}
