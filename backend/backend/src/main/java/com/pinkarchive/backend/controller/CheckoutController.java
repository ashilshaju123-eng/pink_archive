package com.pinkarchive.backend.controller;

import com.pinkarchive.backend.db.*;
import com.pinkarchive.backend.model.Cart;
import com.pinkarchive.backend.model.CartItem;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import com.pinkarchive.backend.db.OrderEntity;
import com.pinkarchive.backend.db.OrderItemEntity;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

@Controller
public class CheckoutController {

    private final OrderRepository orderRepo;
    private final OrderItemRepository orderItemRepo;
    private final ProductRepository productRepo;
    private final VariantRepository variantRepo;

    @Value("${stripe.successUrl}")
    private String successUrl;

    @Value("${stripe.cancelUrl}")
    private String cancelUrl;

    public CheckoutController(OrderRepository orderRepo,
                              OrderItemRepository orderItemRepo,
                              ProductRepository productRepo,
                              VariantRepository variantRepo) {
        this.orderRepo = orderRepo;
        this.orderItemRepo = orderItemRepo;
        this.productRepo = productRepo;
        this.variantRepo = variantRepo;
    }

    private Cart getCart(HttpSession session) {
        Cart cart = (Cart) session.getAttribute("CART");
        if (cart == null) cart = new Cart();
        return cart;
    }

    @PostMapping("/checkout")
    public String startCheckout(HttpSession session) throws StripeException {

        Cart cart = getCart(session);
        if (cart.isEmpty()) return "redirect:/cart";

        // 1) Validate stock before creating order
        for (CartItem ci : cart.getItems()) {

            ProductEntity p = productRepo.findBySlugAndActiveTrue(ci.getProductSlug()).orElse(null);
            if (p == null) return "redirect:/cart";

            VariantEntity v = variantRepo.findByProductAndSize(p, ci.getSize()).orElse(null);
            if (v == null) return "redirect:/cart";

            if (v.getStock() < ci.getQuantity()) {
                // Not enough stock anymore
                return "redirect:/cart";
            }
        }

        // 2) Create Order
        OrderEntity order = new OrderEntity("CREATED", cart.getTotalPence());
        order = orderRepo.save(order);

        // 3) Save order items
        List<OrderItemEntity> items = new ArrayList<>();
        for (CartItem ci : cart.getItems()) {
            items.add(new OrderItemEntity(
                    order,
                    ci.getProductSlug(),
                    ci.getProductName(),
                    ci.getSize(),
                    ci.getUnitPricePence(),
                    ci.getQuantity()
            ));
        }
        orderItemRepo.saveAll(items);

        // 4) Create Stripe session
        List<SessionCreateParams.LineItem> lineItems = new ArrayList<>();
        for (CartItem ci : cart.getItems()) {
            lineItems.add(
                    SessionCreateParams.LineItem.builder()
                            .setQuantity((long) ci.getQuantity())
                            .setPriceData(
                                    SessionCreateParams.LineItem.PriceData.builder()
                                            .setCurrency("gbp")
                                            .setUnitAmount((long) ci.getUnitPricePence())
                                            .setProductData(
                                                    SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                            .setName(ci.getProductName() + " (Size " + ci.getSize() + ")")
                                                            .build()
                                            )
                                            .build()
                            )
                            .build()
            );
        }

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl + "?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(cancelUrl)
                .addAllLineItem(lineItems)
                .putMetadata("orderId", String.valueOf(order.getId()))
                .build();

        Session stripeSession = Session.create(params);

        order.setStripeSessionId(stripeSession.getId());
        orderRepo.save(order);

        return "redirect:" + stripeSession.getUrl();
    }

    @GetMapping("/checkout/success")
    public String success(@RequestParam(required = false) String session_id,
                          Model model,
                          HttpSession httpSession) {

        if (session_id == null || session_id.isBlank()) {
            return "redirect:/shop";
        }

        OrderEntity order = orderRepo.findByStripeSessionId(session_id).orElse(null);
        if (order == null) {
            return "redirect:/shop";
        }

        // Clear cart (user has paid successfully if they landed here)
        Cart cart = getCart(httpSession);
        cart.clear();
        httpSession.setAttribute("CART", cart);

        List<OrderItemEntity> items = orderItemRepo.findByOrder(order);

        model.addAttribute("order", order);
        model.addAttribute("items", items);

        return "checkout-success";
    }
    @GetMapping("/checkout/status")
    @ResponseBody
    public ResponseEntity<String> status(@RequestParam String session_id) {
        Optional<OrderEntity> orderOpt = orderRepo.findByStripeSessionId(session_id);
        if (orderOpt.isEmpty()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(orderOpt.get().getStatus());
    }
}