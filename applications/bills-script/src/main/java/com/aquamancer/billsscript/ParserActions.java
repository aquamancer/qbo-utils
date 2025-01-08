package com.aquamancer.billsscript;

import java.util.List;

public interface ParserActions {
    public Bill parseBill();
    public List<?> getInvoiceNumbersFromText();
    public List<?> getInvoiceNumbersFromArea();
    public List<?> getInvoiceDatesFromText();
    public List<?> getInvoiceDatesFromArea();
}
