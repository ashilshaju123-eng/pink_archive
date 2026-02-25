package com.pinkarchive.backend.controller;

import com.pinkarchive.backend.db.*;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.net.ApiResource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.BufferedReader;
import java.time.Instant;
import java.util.List;

@Controller
public class StripeWebhookController {

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
    public ResponseEntity<String> handleStripeWebhook(HttpServletRequest request) throws Exception {

        String payload = readBody(request);
        String sigHeader = request.getHeader("Stripe-Signature");

        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            return ResponseEntity.status(400).body("Invalid signature");
        }

        // We only care about successful checkout completion
        if ("checkout.session.completed".equals(event.getType())) {
            Session session = (Session) ApiResource.GSON.fromJson(event.getDataObjectDeserializer().getObject().get().toJson(), Session.class);

            String sessionId = session.getId();

            OrderEntity order = orderRepo.findByStripeSessionId(sessionId).orElse(null);
            if (order == null) return ResponseEntity.ok("No matching order");

            // Idempotency: if already paid, do nothing
            if ("PAID".equals(order.getStatus())) {
                return ResponseEntity.ok("Already processed");
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
                if (newStock < 0) newStock = 0; // safety; we’ll improve later
                v.setStock(newStock);
                variantRepo.save(v);
            }
        }

        return ResponseEntity.ok("ok");
    }

    private String readBody(HttpServletRequest request) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }
}