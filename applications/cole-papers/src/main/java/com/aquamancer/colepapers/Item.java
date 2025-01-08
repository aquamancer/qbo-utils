package com.aquamancer.colepapers;

public class Item {
    private boolean include;
    private String UOM, itemName, description;
    private int BO, originalOrder, previousShip, quantity;
    private float unitPrice, totalPrice;
    public Item(boolean include, String UOM, String itemName, String description, int BO, int originalOrder, int previousShip) {
        this.include = include;
        this.UOM = UOM;
        this.itemName = itemName;
        this.description = description;
        this.BO = BO;
        this.originalOrder = originalOrder;
        this.previousShip = previousShip;
    }
    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public float getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(float unitPrice) {
        this.unitPrice = unitPrice;
    }

    public float getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(float totalPrice) {
        this.totalPrice = totalPrice;
    }

    public Item(){
    }


    public boolean isInclude() {
        return include;
    }

    public void setInclude(boolean include) {
        this.include = include;
    }

    public String getUOM() {
        return UOM;
    }

    public void setUOM(String UOM) {
        this.UOM = UOM;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getBO() {
        return BO;
    }

    public void setBO(int BO) {
        this.BO = BO;
    }

    public int getOriginalOrder() {
        return originalOrder;
    }

    public void setOriginalOrder(int originalOrder) {
        this.originalOrder = originalOrder;
    }

    public int getPreviousShip() {
        return previousShip;
    }

    public void setPreviousShip(int previousShip) {
        this.previousShip = previousShip;
    }
}
