package com.aquamancer.invoicematcher.fragment;

import com.aquamancer.invoicematcher.Headers;
import org.apache.commons.csv.CSVRecord;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Fragment {
    private String eftTraceNumber, invoiceNumber, paymentDate;
    private BigDecimal eftAmount;
    private List<BigDecimal> eftAmountMerges;
    private Map<String, BigDecimal> crossInvoiceAdjustments;
    public Fragment(CSVRecord record) {
        this.eftTraceNumber = record.get(Headers.FRAGMENT.get("eftTraceNumber"));
        this.invoiceNumber = record.get(Headers.FRAGMENT.get("invoiceNumber"));
        this.paymentDate = record.get(Headers.FRAGMENT.get("paymentDate"));
        this.eftAmount = new BigDecimal(record.get(Headers.FRAGMENT.get("eftAmount")));
        this.eftAmountMerges = new ArrayList<>();
        this.eftAmountMerges.add(eftAmount);
        this.crossInvoiceAdjustments = new HashMap<>();
    }

    public Fragment(String eftTraceNumber, String invoiceNumber, String paymentDate, BigDecimal eftAmount) {
        this.eftTraceNumber = eftTraceNumber;
        this.invoiceNumber = invoiceNumber;
        this.paymentDate = paymentDate;
        this.eftAmount = eftAmount;
    }

    public String getEftTraceNumber() {
        return eftTraceNumber;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public String getPaymentDate() {
        return paymentDate;
    }

    public BigDecimal getEftAmount() {
        return eftAmount.setScale(2, RoundingMode.HALF_UP);
    }
    public List<BigDecimal> getEftAmountMerges() {
        return this.eftAmountMerges;
    }
    public Map<String, BigDecimal> getCrossInvoiceAdjustments() {
        return this.crossInvoiceAdjustments;
    }
    public void mergeAmounts(Fragment fodder) {
        this.eftAmount = this.eftAmount.add(fodder.getEftAmount());
        eftAmountMerges.add(fodder.getEftAmount());
    }
    public void mergeCrossInvoiceAdjustment(Fragment negativeFragment) {
        this.eftAmount = this.eftAmount.add(negativeFragment.getEftAmount());
        this.crossInvoiceAdjustments.put(negativeFragment.getInvoiceNumber(), negativeFragment.getEftAmount());
    }
}
