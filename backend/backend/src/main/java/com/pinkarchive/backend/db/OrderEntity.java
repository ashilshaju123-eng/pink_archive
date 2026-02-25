package com.pinkarchive.backend.db;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "orders")
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Stripe Checkout Session id (set after we create it)
    @Column(unique = true)
    private String stripeSessionId;

    @Column(nullable = false)
    private String status; // CREATED, PAID, CANCELLED

    @Column(nullable = false)
    private int totalPence;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    // Set when Stripe confirms payment
    private Instant paidAt;

    public OrderEntity() {}

    public OrderEntity(String status, int totalPence) {
        this.status = status;
        this.totalPence = totalPence;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getStripeSessionId() { return stripeSessionId; }
    public String getStatus() { return status; }
    public int getTotalPence() { return totalPence; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getPaidAt() { return paidAt; }

    public void setStripeSessionId(String stripeSessionId) { this.stripeSessionId = stripeSessionId; }
    public void setStatus(String status) { this.status = status; }
    public void setTotalPence(int totalPence) { this.totalPence = totalPence; }
    public void setPaidAt(Instant paidAt) { this.paidAt = paidAt; }

    @Transient
    public String getTotalFormatted() {
        return "£" + String.format("%.2f", totalPence / 100.0);
    }
}