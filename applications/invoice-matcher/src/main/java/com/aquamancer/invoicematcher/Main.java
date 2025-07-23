package com.aquamancer.invoicematcher;

import com.aquamancer.invoicematcher.fragment.FragmentMatcher;
import com.aquamancer.invoicematcher.fragment.Match;
import com.aquamancer.invoicematcher.fragment.MatchMethod;
import com.aquamancer.invoicematcher.uploader.Uploader;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class Main {
    private static final Logger LOGGER = LogManager.getLogger(Main.class);
    public static void main(String[] args) {
        // Get path of .jar file
        String jarPath;
        try {
            jarPath = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParentFile().getAbsolutePath();
            LOGGER.debug("Found .jar path: {}", jarPath);
        } catch (URISyntaxException ex) {
            throw new RuntimeException("Could not parse file path of .jar file.");
        }
        // Load import and export file paths
        PreferencesParser preferences = new PreferencesParser(jarPath);

        // Load bank deposits --
        // Specify header mappings.
        CSVFormat bankDepositFormat = CSVFormat.Builder
                .create()
                .setHeader("Date", "Bank Description", "Spent", "received", "From/To", "Match/Categorize")
                .build();
        // Create filter for bank deposits. Excludes row if false.
        Predicate<CSVRecord> bankDepositFilter = record -> !record.get(Headers.BANK.get("receivedAmount")).isEmpty() && preferences.getBankDepositDescriptionFilter().stream().anyMatch(regex -> record.get(Headers.BANK.get("description")).matches(regex));
        // Create list of file paths to Qbo bank deposit exports.
        List<String> bankDepositFilePaths = preferences.getAllBankDepositFileNames();
        // Merge all bank deposits throughout the files that satisfy the filter.
        List<CSVRecord> bankDeposits = CsvMerger.merge(bankDepositFilePaths, bankDepositFilter, bankDepositFormat);

        // Load myInvoice (fragment) csv
        CSVFormat fragmentFormat = CSVFormat.Builder
                .create()
                .setHeader()
                .build();
        // Create filter for fragments. Excludes row if false.
        Predicate<CSVRecord> fragmentFilter = record ->
                !record.get(Headers.FRAGMENT.get("paymentDate")).isBlank()
                && !record.get(Headers.FRAGMENT.get("eftAmount")).equals("0")
                && !record.get(Headers.FRAGMENT.get("eftAmount")).isBlank();
        // Create a list of file paths to csv to be merged
        List<String> fragmentFilePaths = preferences.getAllFragmentFilePaths();
        // Merge the files.
        List<CSVRecord> fragmentList = CsvMerger.merge(fragmentFilePaths, fragmentFilter, fragmentFormat);

        // Load invoices unpaid
        String invoicesUnpaidPath = preferences.getInvoicesUnpaidFileName();
        Map<String, List<CSVRecord>> invoicesUnpaid = loadInvoicesUnpaid(invoicesUnpaidPath);

        // Initialize Payment and Deposit spreadsheet writers
        Uploader uploader = createUploader(
                preferences.getPaymentExportFilePath(),
                preferences.getDepositExportFilePath(),
                preferences.getPaymentErrorExportFilePath(),
                preferences.getDepositErrorExportFilePath(),
                invoicesUnpaid
        );

        // Match bank deposit lump sums with fragmented payments in myInvoice.csv
        List<Match> matches = new ArrayList<>();
        for (CSVRecord bankDeposit : bankDeposits) {
            Match match = FragmentMatcher.calculateFragmentMatches(bankDeposit, fragmentList);
            matches.add(match);
            if (match.method() != MatchMethod.NO_MATCH) {
                uploader.append(match.fragments());
            }
        }
        uploader.export();
        writeSummary(preferences.getSummaryExportFilePath(), matches, fragmentFilePaths, bankDepositFilePaths, invoicesUnpaidPath);
    }
    // begin and end dates for every file
    private static Map<String, List<CSVRecord>> loadInvoicesUnpaid(String filePath) {
        try (FileReader reader = new FileReader(filePath)) {
            return InvoicesUnpaidParser.invoicesUnpaidToMap(reader);
        } catch (IOException ex) {
            throw new RuntimeException("Could not load invoices unpaid!");
        }
    }
    private static Uploader createUploader(String paymentsExportFilePath, String depositsExportFilePath, String paymentsErrorExportFilePath, String depositsErrorExportFilePath, Map<String, List<CSVRecord>> invoicesUnpaid) {
        try {
            // These streams will automatically close after the Printers close.
            FileWriter paymentsWriter = new FileWriter(paymentsExportFilePath);
            FileWriter depositsWriter = new FileWriter(depositsExportFilePath);
            FileWriter paymentsErrorWriter = new FileWriter(paymentsErrorExportFilePath);
            FileWriter depositsErrorWriter = new FileWriter(depositsErrorExportFilePath);

            return new Uploader(paymentsWriter, depositsWriter, paymentsErrorWriter, depositsErrorWriter, invoicesUnpaid);
        } catch (IOException ex) {
            throw new RuntimeException("Could not create payment uploader!");
        }
    }
    private static void writeSummary(String filePath, List<Match> matches, List<String> fragmentFilePaths, List<String> bankDepositFilePaths, String invoicesUnpaidFilePath) {
        try (FileWriter summaryWriter = new FileWriter(filePath)) {
            summaryWriter.append(SummaryGenerator.generateSummary(matches, fragmentFilePaths, bankDepositFilePaths, invoicesUnpaidFilePath));
        } catch (IOException ex) {
            throw new RuntimeException("Could not write summary to " + filePath);
        }
    }
}
