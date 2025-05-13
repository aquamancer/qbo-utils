package com.aquamancer.invoicematcher.uploader;

import com.aquamancer.invoicematcher.fragment.Fragment;
import com.aquamancer.invoicematcher.fragment.FragmentMatcher;
import com.aquamancer.invoicematcher.Headers;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

public class UploaderEntry {
    private static final Logger LOGGER = LogManager.getLogger(UploaderEntry.class);
    private int paymentIndex, depositIndex;
    private LocalDate TxnDate;
    private String TxnDatePrint;
    private String Customer, PrivateNote, InvoiceApplyTo, LineAmount;
    private StringBuilder PaymentRefNumberPayments;
    private String PaymentRefNumberDeposits; // eft trace
    private boolean hasError = false;

    // Payments constants
    private static final String DEPOSIT_TO_ACCOUNT_PAYMENTS = "Undeposited Funds";
    // Deposits constants
    private static final String DEPOSIT_TO_ACCOUNT_DEPOSITS = "Pacific Premier Bank (4891)";
    private static final String LINKED_TXN_TYPE = "Payment";
    // private static final String ERROR_INDICATOR = "\uFFFD".repeat(2) + "ERROR: "; // \uFFFD is the diamond with ? symbol
    private static final String ERROR_INDICATOR = "ERROR: ";

    public UploaderEntry(Fragment fragment, int paymentIndex, int depositIndex, Map<String, List<CSVRecord>> invoicesUnpaid) {
        this.paymentIndex = paymentIndex;
        this.depositIndex = depositIndex;
        updateInvoiceDate(fragment);
        updateCustomer(fragment.getInvoiceNumber(), invoicesUnpaid);
        updateFragmentData(fragment);
        updatePaymentRefNumber(fragment);
    }
    private void updateFragmentData(Fragment fragment) {
        this.PrivateNote = fragment.getEftTraceNumber();
        // Write memo for same fragment invoice number merges
        List<BigDecimal> fragmentAmountMerges = fragment.getEftAmountMerges();
        if (fragmentAmountMerges.size() > 1) {
            this.PrivateNote += "\nCombined " + fragmentAmountMerges.size() + " invoice lines:";
            for (BigDecimal amount : fragmentAmountMerges) {
                this.PrivateNote += " " + amount.toString();
            }
        }
        // write memo for cross-invoice negative adjustments
        Map<String, BigDecimal> negativeAdjustments = fragment.getCrossInvoiceAdjustments();
        if (!negativeAdjustments.isEmpty()) {
            this.PrivateNote += "\nCross-invoice adjustments: ";
            for (Map.Entry<String, BigDecimal> adjustment : negativeAdjustments.entrySet()) {
                this.PrivateNote += "Invoice " + adjustment.getKey() + " for " + adjustment.getValue();
            }
        }

        String fragmentInvoiceNumber = fragment.getInvoiceNumber();
        if (isNumberAppendedWithLetter(fragmentInvoiceNumber)) {
            LOGGER.info("Detected invoice number appended with letter: {}, using {} for InvoiceApplyTo", fragmentInvoiceNumber, fragmentInvoiceNumber.substring(0, fragmentInvoiceNumber.length() - 1));
            this.InvoiceApplyTo = fragmentInvoiceNumber.substring(0, fragmentInvoiceNumber.length() - 1);
        } else {
            this.InvoiceApplyTo = fragmentInvoiceNumber;
        }

        this.LineAmount = fragment.getEftAmount().toString();
        if (fragment.getEftTraceNumber().isEmpty()) {
            this.PaymentRefNumberDeposits = ERROR_INDICATOR + "Eft trace number does not exist!";
            this.hasError = true;
        } else {
            this.PaymentRefNumberDeposits = fragment.getEftTraceNumber();
        }
        // the below breaks PaymentRefNumber
//        if (this.PrivateNote.isEmpty()) this.PrivateNote = ERROR_INDICATOR + "Field is blank in myInvoice";
//        if (this.InvoiceApplyTo.isEmpty()) this.InvoiceApplyTo = ERROR_INDICATOR + "Field is blank in myInvoice";
//        if (this.LineAmount.isEmpty()) this.LineAmount = ERROR_INDICATOR + "Field is blank in myInvoice";
    }
    private void updateInvoiceDate(Fragment fragment) {
        try {
            String invoiceDate = fragment.getPaymentDate();
            this.TxnDate = LocalDate.parse(invoiceDate, FragmentMatcher.DATE_FORMAT);
            this.TxnDatePrint = TxnDate.toString();
        } catch (DateTimeParseException ex) {
            this.TxnDate = null;
            this.TxnDatePrint = ERROR_INDICATOR + "Could not parse invoice date from fragment";
            this.hasError = true;
        }
    }
    private void updateCustomer(String invoiceNumber, Map<String, List<CSVRecord>> invoicesUnpaid) {
        if (invoicesUnpaid.containsKey(invoiceNumber) && !invoicesUnpaid.get(invoiceNumber).isEmpty()) {
            List<CSVRecord> invoiceGroup = invoicesUnpaid.get(invoiceNumber);
            if (invoiceGroup.size() > 1) LOGGER.warn("Multiple invoices unpaid found with invoice number: {}. Grabbing customer from the first occurrence.", invoiceNumber);
            this.Customer = invoiceGroup.getFirst().get(Headers.INVOICES_UNPAID.get("customer"));
        } else {
            if (isNumberAppendedWithLetter(invoiceNumber)) {
                String trimmedQuery = invoiceNumber.substring(0, invoiceNumber.length() - 1);
                LOGGER.debug("Detected invoice no: {} appended with one letter. Attempting to find invoices unpaid with invoice number: {}", invoiceNumber, trimmedQuery);
                updateCustomer(trimmedQuery, invoicesUnpaid);
            } else {
                LOGGER.error("Could not find invoice number: {} in invoices unpaid.", invoiceNumber);
                this.Customer = ERROR_INDICATOR + "Invoice number does not exist in invoices unpaid";
                this.hasError = true;
            }
        }
    }

    private void updatePaymentRefNumber(Fragment fragment) {
        // Checks
        String eftTraceNumber = fragment.getEftTraceNumber();
        if (this.TxnDate == null || eftTraceNumber.isEmpty()) {
            this.PaymentRefNumberPayments = new StringBuilder(ERROR_INDICATOR + "TxnDate or PrivateNote(eftTraceNo) is null");
            this.hasError = true;
        } else {
            this.PaymentRefNumberPayments = new StringBuilder();
            PaymentRefNumberPayments.append(TxnDate.getYear())
                    .append(digitPlaceholders(TxnDate.getMonthValue(), 2))
                    .append(digitPlaceholders(TxnDate.getDayOfMonth(), 2))
                    .append(eftTraceNumber.substring(eftTraceNumber.length() - 4))
                    .append(digitPlaceholders(paymentIndex, 3));
        }
    }
    private static boolean isNumberAppendedWithLetter(String query) {
        boolean lastCharIsLetter = Character.isLetter(query.charAt(query.length() - 1));
        String trimmedQuery = query.substring(0, query.length() - 1);
        return lastCharIsLetter && canParseInt(trimmedQuery);
    }

    public static StringBuilder digitPlaceholders(int num, int digits) {
        if (digits < 1 || num < 0) {
            throw new RuntimeException("Tried to create a string with " + digits + " digits for num: " + num);
        }
        // Make sure that num does not have more digits than digits.
        int maximumNum = (int) Math.pow(10, digits) - 1;
        if (num > maximumNum) {
            LOGGER.warn("Number to create a string with {} digits is greater than {} digits. digits: {}, num: {}", digits, digits, digits, num);
            return new StringBuilder(num);
        }

        // Calculate number of zeroes to prepend
        int numZeroesToPrepend = digits - numDigits(num);

        StringBuilder result = new StringBuilder();
        result.append("0".repeat(numZeroesToPrepend));
        result.append(num);

        return result;
    }
    private static int numDigits(int num) {
        if (num == 0) {
            return 1; // 0 has one digit.
        }
        return (int) Math.log10(num) + 1;
    }
    public void printPaymentRecord(CSVPrinter printer) {
        try {
            printer.printRecord(paymentIndex, TxnDatePrint, PaymentRefNumberPayments, Customer, null, PrivateNote, DEPOSIT_TO_ACCOUNT_PAYMENTS, InvoiceApplyTo, LineAmount, null, null, null);
        } catch (IOException ex) {
            throw new RuntimeException("IOException whilst printing payment line.");
        }
    }
    public void printDepositRecord(CSVPrinter printer) {
        try {
            printer.printRecord(
                    depositIndex,
                    TxnDate,
                    null,
                    null,
                    null,
                    DEPOSIT_TO_ACCOUNT_DEPOSITS,
                    null, null, null, null, null, null,
                    PaymentRefNumberDeposits,
                    null,
                    LINKED_TXN_TYPE,
                    PaymentRefNumberPayments,
                    null, null, null
            );
        } catch (IOException ex) {
            throw new RuntimeException("IOException whilst printing deposit line.");
        }
    }


    private static boolean canParseInt(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public boolean hasError() {
        return this.hasError;
    }

    public static void main(String[] args) {
        System.out.println(digitPlaceholders(0, 3));
        System.out.println(digitPlaceholders(1, 1));
        System.out.println(digitPlaceholders(0, 1));
        System.out.println(digitPlaceholders(9, 1));
        System.out.println(digitPlaceholders(11, 3));
        System.out.println(digitPlaceholders(999, 4));
        System.out.println(digitPlaceholders(0, 1));
        System.out.println(digitPlaceholders(0, 0));
    }
}
