package com.pinkarchive.backend.db;

import jakarta.persistence.*;

@Entity
@Table(name = "variants",
        uniqueConstraints = @UniqueConstraint(name = "uk_product_size", columnNames = {"product_id", "size"}))
public class VariantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    @Column(nullable = false, length = 4)
    private String size; // S, M, L

    @Column(nullable = false)
    private int stock;

    public VariantEntity() {}

    public VariantEntity(ProductEntity product, String size, int stock) {
        this.product = product;
        this.size = size;
        this.stock = stock;
    }

    public Long getId() { return id; }
    public ProductEntity getProduct() { return product; }
    public String getSize() { return size; }
    public int getStock() { return stock; }

    public void setProduct(ProductEntity product) { this.product = product; }
    public void setSize(String size) { this.size = size; }
    public void setStock(int stock) { this.stock = stock; }

    @Transient
    public boolean isInStock() {
        return stock > 0;
    }
}