package com.pinkarchive.backend.db;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products")
public class ProductEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    private int pricePence;

    @Column(nullable = false)
    private String imageUrl;

    @Column(nullable = false)
    private boolean active = true;

    // Relationship: Product -> Variants (S/M/L)
    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<VariantEntity> variants = new ArrayList<>();

    public ProductEntity() {}

    public ProductEntity(String name, String slug, int pricePence, String imageUrl) {
        this.name = name;
        this.slug = slug;
        this.pricePence = pricePence;
        this.imageUrl = imageUrl;
        this.active = true;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getSlug() { return slug; }
    public int getPricePence() { return pricePence; }
    public String getImageUrl() { return imageUrl; }
    public boolean isActive() { return active; }

    public List<VariantEntity> getVariants() { return variants; }

    public void setName(String name) { this.name = name; }
    public void setSlug(String slug) { this.slug = slug; }
    public void setPricePence(int pricePence) { this.pricePence = pricePence; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setActive(boolean active) { this.active = active; }

    @Transient
    public String getPriceFormatted() {
        return "£" + String.format("%.2f", pricePence / 100.0);
    }

    @Transient
    public boolean isSoldOut() {
        if (variants == null || variants.isEmpty()) return true;
        return variants.stream().allMatch(v -> v.getStock() <= 0);
    }
}