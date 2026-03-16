package com.pinkarchive.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pinkarchive.backend.db.*;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

@Controller
public class StripeWebhookController {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Value("${stripe.webhookSecret}")
    private String webhookSecret;

    private final OrderRepository orderRepo;
    private final OrderItemRepository orderItemRepo;
    private final ProductRepository productRepo;
    private final VariantRepository variantRepo;

    public StripeWebhookController(OrderRepository orderRepo,
                                   OrderItemRepository orderItemRepo,
                                   ProductRepository productRepo,
                                   VariantRepository variantRepo) {
        this.orderRepo = orderRepo;
        this.orderItemRepo = orderItemRepo;
        this.productRepo = productRepo;
        this.variantRepo = variantRepo;
    }

    @PostMapping("/webhook/stripe")
    @ResponseBody
    @Transactional
    public ResponseEntity<String> handle(HttpServletRequest request) throws Exception {

        byte[] bytes = request.getInputStream().readAllBytes();
        String payload = new String(bytes, StandardCharsets.UTF_8);

        String sigHeader = request.getHeader("Stripe-Signature");
        if (sigHeader == null || sigHeader.isBlank()) {
            return ResponseEntity.status(400).body("Missing signature");
        }
        if (webhookSecret == null || webhookSecret.isBlank() || !webhookSecret.startsWith("whsec_")) {
            log.warn("Webhook secret not configured. Check STRIPE_WEBHOOK_SECRET env var.");
            return ResponseEntity.status(400).body("Webhook secret not configured");
        }

        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.warn("Stripe signature verification failed: {}", e.getMessage());
            return ResponseEntity.status(400).body("Invalid signature");
        }

        // Only care about this event
        if (!"checkout.session.completed".equals(event.getType())) {
            return ResponseEntity.ok("ignored");
        }

        // ✅ Parse session + metadata from RAW JSON payload (no Stripe deserializer needed)
        JsonNode root = MAPPER.readTree(payload);
        JsonNode obj = root.path("data").path("object");

        String sessionId = obj.path("id").asText(null);
        String orderIdStr = obj.path("metadata").path("orderId").asText(null);

        log.info("checkout.session.completed sessionId={} metadata.orderId={}", sessionId, orderIdStr);

        if (sessionId == null || sessionId.isBlank()) {
            log.warn("No session id in payload");
            return ResponseEntity.ok("no session id");
        }

        OrderEntity order = null;

        // Prefer metadata orderId (most reliable)
        if (orderIdStr != null && !orderIdStr.isBlank()) {
            try {
                Long orderId = Long.parseLong(orderIdStr);
                order = orderRepo.findById(orderId).orElse(null);
            } catch (NumberFormatException ignored) {
                // fall back below
            }
        }

        // Fallback: find by session id
        if (order == null) {
            order = orderRepo.findByStripeSessionId(sessionId).orElse(null);
        }

        log.info("Order found={}", (order != null));

        if (order == null) {
            log.warn("No matching order for session {}", sessionId);
            return ResponseEntity.ok("no matching order");
        }

        // Ensure order has session id stored (handy if found by orderId)
        if (order.getStripeSessionId() == null || order.getStripeSessionId().isBlank()) {
            order.setStripeSessionId(sessionId);
        }

        // Idempotent
        if ("PAID".equals(order.getStatus())) {
            return ResponseEntity.ok("already processed");
        }

        // Mark paid
        order.setStatus("PAID");
        order.setPaidAt(Instant.now());
        orderRepo.save(order);

        // Decrement stock
        List<OrderItemEntity> items = orderItemRepo.findByOrder(order);
        for (OrderItemEntity item : items) {

            ProductEntity p = productRepo.findBySlugAndActiveTrue(item.getProductSlug()).orElse(null);
            if (p == null) continue;

            VariantEntity v = variantRepo.findByProductAndSize(p, item.getSize()).orElse(null);
            if (v == null) continue;

            int newStock = v.getStock() - item.getQuantity();
            if (newStock < 0) newStock = 0;

            v.setStock(newStock);
            variantRepo.save(v);
        }

        log.info("Order {} marked PAID and stock decremented", order.getId());
        return ResponseEntity.ok("processed");
    }
}