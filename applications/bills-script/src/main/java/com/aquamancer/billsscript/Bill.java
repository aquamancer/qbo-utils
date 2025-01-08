package com.aquamancer.billsscript;

import com.aquamancer.currency.Currency;

public abstract class Bill {
    private String invoiceNumber, invoiceDate;
    private Currency currency;
    public Bill(String invoiceNumber, String invoiceDate, Currency currency) {
        this.invoiceNumber = invoiceNumber;
        this.invoiceDate = invoiceDate;
        this.currency = currency;
    }
}
