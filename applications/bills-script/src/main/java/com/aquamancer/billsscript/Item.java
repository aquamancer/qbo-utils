package com.aquamancer.billsscript;

public class Item {
    public String category, name, description;
    public double amount, price, total;

    public Item(String name, double amount, double price, double total) {
        this.name = name;
        this.amount = amount;
        this.price = price;
        this.total = total;
    }
}
