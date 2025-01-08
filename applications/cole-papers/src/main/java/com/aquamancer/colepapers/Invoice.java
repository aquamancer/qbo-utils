package com.aquamancer.colepapers;

import java.util.ArrayList;
public class Invoice {
    private float grandTotal, subtotal, salesTax;
    private String invoiceNumber, invoiceDate, customer;
    private ArrayList<Item> items;

    public Invoice() {
        this.items = new ArrayList<>();
    }

    public float getGrandTotal() {
        return grandTotal;
    }

    public void setGrandTotal(float grandTotal) {
        this.grandTotal = grandTotal;
    }

    public float getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(float subtotal) {
        this.subtotal = subtotal;
    }

    public float getSalesTax() {
        return salesTax;
    }

    public void setSalesTax(float salesTax) {
        this.salesTax = salesTax;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public String getInvoiceDate() {
        return invoiceDate;
    }

    public void setInvoiceDate(String invoiceDate) {
        this.invoiceDate = invoiceDate;
    }

    public String getCustomer() {
        return customer;
    }

    public void setCustomer(String customer) {
        this.customer = customer;
    }
    public void addItem(Item item) {
        this.items.add(item);
    }
    public ArrayList<Item> getItems() {
        return this.items;
    }
}
