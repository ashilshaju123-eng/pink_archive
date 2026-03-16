package com.pinkarchive.backend.db;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    Optional<OrderEntity> findByStripeSessionId(String stripeSessionId);
    List<OrderEntity> findAllByOrderByCreatedAtDesc();
}