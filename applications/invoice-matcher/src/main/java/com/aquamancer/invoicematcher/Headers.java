package com.aquamancer.invoicematcher;

import java.util.Map;

public final class Headers {
    public static final Map<String, String> FRAGMENT = Map.of(
            "eftTraceNumber", "EFT Trace No.",
            "invoiceNumber", "Invoice No.",
            "paymentDate", "Payment Date",
            "eftAmount", "Check EFT Amount"
    );
    public static final Map<String, String> BANK = Map.of(
            "date", "Date",
            "description", "Bank Description",
            "receivedAmount", "received"
    );
    public static final Map<String, String> INVOICES_UNPAID = Map.of(
            "invoiceDate", "Date",
            "invoiceNumber", "Num",
            "customer", "Name",
            "amount", "Amount",
            "openBalance", "Open balance"
    );
}
