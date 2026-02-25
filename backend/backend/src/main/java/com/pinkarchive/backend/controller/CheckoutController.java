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

@Controller
public class CheckoutController {

    private final OrderRepository orderRepo;
    private final OrderItemRepository orderItemRepo;

    @Value("${stripe.successUrl}")
    private String successUrl;

    @Value("${stripe.cancelUrl}")
    private String cancelUrl;

    public CheckoutController(OrderRepository orderRepo, OrderItemRepository orderItemRepo) {
        this.orderRepo = orderRepo;
        this.orderItemRepo = orderItemRepo;
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

        // 1) Create Order in DB
        OrderEntity order = new OrderEntity("CREATED", cart.getTotalPence());
        order = orderRepo.save(order);

        // 2) Save order items
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

        // 3) Create Stripe Checkout Session
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
                // optional: collect email
                .setCustomerEmail(null)
                .addAllLineItem(lineItems)
                // store our order id in Stripe metadata so webhook can find it later
                .putMetadata("orderId", String.valueOf(order.getId()))
                .build();

        Session stripeSession = Session.create(params);

        // 4) Save session id on order
        order.setStripeSessionId(stripeSession.getId());
        orderRepo.save(order);

        // 5) Redirect user to Stripe
        return "redirect:" + stripeSession.getUrl();
    }

    @GetMapping("/checkout/success")
    public String success(@RequestParam(required = false) String session_id,
                          Model model,
                          HttpSession httpSession) {
        // For now: clear cart on success page view (webhook will be the real source of truth later)
        Cart cart = getCart(httpSession);
        cart.clear();
        httpSession.setAttribute("CART", cart);

        model.addAttribute("sessionId", session_id);
        return "checkout-success";
    }
}