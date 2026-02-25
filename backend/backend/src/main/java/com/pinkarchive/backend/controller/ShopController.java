package com.pinkarchive.backend.controller;

import com.pinkarchive.backend.db.*;
import com.pinkarchive.backend.model.Cart;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class ShopController {

    private final ProductRepository productRepository;
    private final VariantRepository variantRepository;

    public ShopController(ProductRepository productRepository, VariantRepository variantRepository) {
        this.productRepository = productRepository;
        this.variantRepository = variantRepository;
    }

    private Cart getCart(HttpSession session) {
        Cart cart = (Cart) session.getAttribute("CART");
        if (cart == null) {
            cart = new Cart();
            session.setAttribute("CART", cart);
        }
        return cart;
    }

    @GetMapping("/shop")
    public String shop(Model model) {
        model.addAttribute("products", productRepository.findByActiveTrue());
        return "shop";
    }

    @GetMapping("/product/{slug}")
    public String product(@PathVariable String slug, Model model) {
        ProductEntity product = productRepository.findBySlugAndActiveTrue(slug).orElse(null);
        if (product == null) return "redirect:/shop";

        // Load all variants (S/M/L) + stock
        List<VariantEntity> variants = variantRepository.findByProductOrderBySizeAsc(product);
        variants.sort((a, b) -> {
            java.util.List<String> order = java.util.List.of("S", "M", "L");
            return Integer.compare(order.indexOf(a.getSize()), order.indexOf(b.getSize()));
        });
        model.addAttribute("product", product);
        model.addAttribute("variants", variants);
        return "product";
    }

    @PostMapping("/cart/add")
    public String addToCart(@RequestParam String slug,
                            @RequestParam String size,
                            HttpSession session) {

        ProductEntity product = productRepository.findBySlugAndActiveTrue(slug).orElse(null);
        if (product == null) return "redirect:/shop";

        VariantEntity variant = variantRepository.findByProductAndSize(product, size).orElse(null);
        if (variant == null) return "redirect:/product/" + slug;

        // Security / correctness: prevent adding out-of-stock
        if (variant.getStock() <= 0) return "redirect:/product/" + slug;

        getCart(session).addOrIncrement(product.getSlug(), product.getName(), size, product.getPricePence());
        return "redirect:/cart";
    }

    @GetMapping("/cart")
    public String cart(Model model, HttpSession session) {
        model.addAttribute("cart", getCart(session));
        return "cart";
    }

    @PostMapping("/cart/clear")
    public String clearCart(HttpSession session) {
        getCart(session).clear();
        return "redirect:/cart";
    }
}
