package com.pinkarchive.backend.controller;

import com.pinkarchive.backend.model.Product;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class ShopController {

    // Mocked products
    private static final List<Product> PRODUCTS = List.of(
            new Product("Blush Matching Set", "blush-matching-set", 2999, "https://via.placeholder.com/600x800"),
            new Product("Baby Pink Lounge Set", "baby-pink-lounge-set", 3499, "https://via.placeholder.com/600x800"),
            new Product("Pastel Zip Set", "pastel-zip-set", 3999, "https://via.placeholder.com/600x800"),
            new Product("Soft Pink Hoodie Set", "soft-pink-hoodie-set", 4299, "https://via.placeholder.com/600x800"),
            new Product("Archive Rib Set", "archive-rib-set", 2799, "https://via.placeholder.com/600x800"),
            new Product("Coquette Set", "coquette-set", 3199, "https://via.placeholder.com/600x800")
    );

    @GetMapping("/shop")
    public String shop(Model model) {
        model.addAttribute("products", PRODUCTS);
        return "shop";
    }
}
