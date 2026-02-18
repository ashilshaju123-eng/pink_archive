package com.pinkarchive.backend.controller;

import com.pinkarchive.backend.model.Cart;
import com.pinkarchive.backend.model.Product;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class ShopController {
    //Mocked data

    private static final List<Product> PRODUCTS = List.of(
            new Product("Blush Matching Set", "blush-matching-set", 2999, "https://via.placeholder.com/600x800"),
            new Product("Baby Pink Lounge Set", "baby-pink-lounge-set", 3499, "https://via.placeholder.com/600x800"),
            new Product("Pastel Zip Set", "pastel-zip-set", 3999, "https://via.placeholder.com/600x800"),
            new Product("Soft Pink Hoodie Set", "soft-pink-hoodie-set", 4299, "https://via.placeholder.com/600x800"),
            new Product("Archive Rib Set", "archive-rib-set", 2799, "https://via.placeholder.com/600x800"),
            new Product("Coquette Set", "coquette-set", 3199, "https://via.placeholder.com/600x800")
    );

    private Cart getCart(HttpSession session) {
        Cart cart = (Cart) session.getAttribute("CART");
        if (cart == null) {
            cart = new Cart();
            session.setAttribute("CART", cart);
        }
        return cart;
    }

    private Product findProduct(String slug) {
        return PRODUCTS.stream().filter(p -> p.getSlug().equals(slug)).findFirst().orElse(null);
    }

    @GetMapping("/shop")
    public String shop(Model model) {
        model.addAttribute("products", PRODUCTS);
        return "shop";
    }

    @GetMapping("/product/{slug}")
    public String product(@PathVariable String slug, Model model) {
        Product found = findProduct(slug);
        if (found == null) return "redirect:/shop";

        model.addAttribute("product", found);
        model.addAttribute("sizes", List.of("S", "M", "L"));
        return "product";
    }

    @PostMapping("/cart/add")
    public String addToCart(@RequestParam String slug, @RequestParam String size, HttpSession session) {
        Product found = findProduct(slug);
        if (found == null) return "redirect:/shop";

        if (!List.of("S", "M", "L").contains(size)) return "redirect:/product/" + slug;

        getCart(session).addOrIncrement(found.getSlug(), found.getName(), size, found.getPricePence());
        return "redirect:/cart";
    }

    @GetMapping("/cart")
    public String cart(Model model, HttpSession session) {
        Cart cart = getCart(session);
        model.addAttribute("cart", cart);
        return "cart";
    }

    @PostMapping("/cart/clear")
    public String clearCart(HttpSession session) {
        getCart(session).clear();
        return "redirect:/cart";
    }
}
