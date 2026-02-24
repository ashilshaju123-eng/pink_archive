package com.pinkarchive.backend.controller.dto;

public class ProductForm {

    private String name;
    private String slug;
    private String imageUrl;
    private int pricePence;

    private int stockS;
    private int stockM;
    private int stockL;

    public ProductForm() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public int getPricePence() { return pricePence; }
    public void setPricePence(int pricePence) { this.pricePence = pricePence; }

    public int getStockS() { return stockS; }
    public void setStockS(int stockS) { this.stockS = stockS; }

    public int getStockM() { return stockM; }
    public void setStockM(int stockM) { this.stockM = stockM; }

    public int getStockL() { return stockL; }
    public void setStockL(int stockL) { this.stockL = stockL; }
}