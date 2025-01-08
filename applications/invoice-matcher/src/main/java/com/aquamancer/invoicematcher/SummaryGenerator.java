package com.aquamancer.invoicematcher;

import com.aquamancer.invoicematcher.fragment.Match;
import com.aquamancer.invoicematcher.fragment.MatchMethod;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class SummaryGenerator {
    public static StringBuilder generateSummary(List<Match> matches, List<String> fragmentFilePaths, List<String> bankDepositFilePaths, String invoicesUnpaidFilePath) {
        StringBuilder builder = new StringBuilder();
        int LINE_LENGTH = 75;

        builder.append("Invoice Matcher executed on: ");
        builder.append(LocalDate.now()).append(' ').append(LocalTime.now()).append('\n');

        builder.append(padWithDashes("INPUT SOURCES", LINE_LENGTH));
        builder.append("Fragment input source(s): \n");
        for (String fragmentFilePath : fragmentFilePaths) {
            builder.append(fragmentFilePath).append('\n');
        }
        builder.append('\n');
        builder.append("Bank deposit input source(s): \n");
        for (String bankDepositFilePath : bankDepositFilePaths) {
            builder.append(bankDepositFilePath).append('\n');
        }
        builder.append('\n');
        builder.append("Invoices unpaid file: ").append('\n');
        builder.append(invoicesUnpaidFilePath).append('\n');

        // Group matches into successes and failures
        List<Match> successes = new ArrayList<>();
        List<Match> failures = new ArrayList<>();
        for (Match match : matches) {
            if (match.method() == MatchMethod.NO_MATCH) {
                failures.add(match);
            } else {
                successes.add(match);
            }
        }
        builder.append(padWithDashes("MATCH SUCCESSES", LINE_LENGTH));
        successes.forEach(match -> builder.append(matchMessage(match)).append('\n'));
        builder.append(padWithDashes("MATCH FAILURES", LINE_LENGTH));
        failures.forEach(failure -> builder.append(matchMessage(failure)).append('\n'));

        builder.append(padWithDashes("SUMMARY", LINE_LENGTH));
        builder.append("Total bank deposits: ").append(matches.size()).append('\n');
        builder.append("Successful matches: ").append(successes.size()).append('\n');
        builder.append("Failed matches: ").append(failures.size()).append('\n');

        return builder;
    }
    private static StringBuilder matchMessage(Match match) {
        StringBuilder builder = new StringBuilder();
        builder.append(LocalDate.parse(match.bankDeposit().get(Headers.BANK.get("date")), BankDepositParser.DATE_FORMAT));
        builder.append('\t');
        builder.append(match.bankDeposit().get(Headers.BANK.get("receivedAmount")));
        builder.append("\t\t\t").append(match.method().name());

        return builder;
    }

    private static String padWithDashes(String input, int length) {
        // Calculate the required total length of the dashes and spaces
        int dashesLength = length - input.length() - 2; // Subtract 2 for the spaces before and after the string
        if (dashesLength < 0) {
            // If the input is too long to fit within the specified length, return the input truncated
            return input.substring(0, length);
        }

        // Create the dashes string
        String dashes = "-".repeat(dashesLength / 2);

        // Return the formatted string with spaces and dashes
        return dashes + " " + input + " " + dashes + "\n";
    }
}
