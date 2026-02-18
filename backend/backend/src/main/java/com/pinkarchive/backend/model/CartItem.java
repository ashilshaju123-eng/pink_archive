package com.pinkarchive.backend.model;

public class CartItem {
    private final String productSlug;
    private final String productName;
    private final String size;
    private final int unitPricePence;
    private int quantity;

    public CartItem(String productSlug, String productName, String size, int unitPricePence, int quantity) {
        this.productSlug = productSlug;
        this.productName = productName;
        this.size = size;
        this.unitPricePence = unitPricePence;
        this.quantity = quantity;
    }

    public String getProductSlug() { return productSlug; }
    public String getProductName() { return productName; }
    public String getSize() { return size; }
    public int getUnitPricePence() { return unitPricePence; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public int getLineTotalPence() {
        return unitPricePence * quantity;
    }

    public String getUnitPriceFormatted() {
        return "£" + String.format("%.2f", unitPricePence / 100.0);
    }

    public String getLineTotalFormatted() {
        return "£" + String.format("%.2f", getLineTotalPence() / 100.0);
    }
}
