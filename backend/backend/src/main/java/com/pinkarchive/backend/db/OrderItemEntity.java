package com.pinkarchive.backend.db;

import jakarta.persistence.*;

@Entity
@Table(name = "order_items")
public class OrderItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderEntity order;

    @Column(nullable = false)
    private String productSlug;

    @Column(nullable = false)
    private String productName;

    @Column(nullable = false, length = 4)
    private String size;

    @Column(nullable = false)
    private int unitPricePence;

    @Column(nullable = false)
    private int quantity;

    public OrderItemEntity() {}

    public OrderItemEntity(OrderEntity order, String productSlug, String productName, String size, int unitPricePence, int quantity) {
        this.order = order;
        this.productSlug = productSlug;
        this.productName = productName;
        this.size = size;
        this.unitPricePence = unitPricePence;
        this.quantity = quantity;
    }

    public Long getId() { return id; }
    public OrderEntity getOrder() { return order; }
    public String getProductSlug() { return productSlug; }
    public String getProductName() { return productName; }
    public String getSize() { return size; }
    public int getUnitPricePence() { return unitPricePence; }
    public int getQuantity() { return quantity; }

    public int getLineTotalPence() { return unitPricePence * quantity; }

    @Transient
    public String getLineTotalFormatted() {
        return "£" + String.format("%.2f", getLineTotalPence() / 100.0);
    }
}