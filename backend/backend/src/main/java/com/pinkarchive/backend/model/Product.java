package com.pinkarchive.backend.model;

public class Product {
    private final String name;
    private final String slug;
    private final int pricePence;
    private final String imageUrl;

    public Product(String name, String slug, int pricePence, String imageUrl) {
        this.name = name;
        this.slug = slug;
        this.pricePence = pricePence;
        this.imageUrl = imageUrl;
    }

    public String getName() { return name; }
    public String getSlug() { return slug; }
    public int getPricePence() { return pricePence; }
    public String getImageUrl() { return imageUrl; }

    public String getPriceFormatted() {
        return "£" + String.format("%.2f", pricePence / 100.0);
    }
}
