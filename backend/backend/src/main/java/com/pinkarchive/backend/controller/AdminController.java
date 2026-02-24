package com.pinkarchive.backend.controller;

import com.pinkarchive.backend.controller.dto.ProductForm;
import com.pinkarchive.backend.db.ProductEntity;
import com.pinkarchive.backend.db.ProductRepository;
import com.pinkarchive.backend.db.VariantEntity;
import com.pinkarchive.backend.db.VariantRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final ProductRepository productRepo;
    private final VariantRepository variantRepo;

    public AdminController(ProductRepository productRepo, VariantRepository variantRepo) {
        this.productRepo = productRepo;
        this.variantRepo = variantRepo;
    }

    @GetMapping
    public String adminHome() {
        return "admin";
    }

    @GetMapping("/products")
    public String products(Model model) {
        model.addAttribute("products", productRepo.findAll());
        return "admin-products";
    }

    @GetMapping("/products/new")
    public String newProduct(Model model) {
        ProductForm form = new ProductForm();
        form.stockS = 10;
        form.stockM = 10;
        form.stockL = 10;
        model.addAttribute("form", form);
        return "admin-product-new";
    }

    @PostMapping("/products/new")
    public String createProduct(@ModelAttribute("form") ProductForm form) {

        // Minimal validation
        if (form.name == null || form.name.isBlank()) return "redirect:/admin/products/new";
        if (form.slug == null || form.slug.isBlank()) return "redirect:/admin/products/new";
        if (form.imageUrl == null || form.imageUrl.isBlank()) return "redirect:/admin/products/new";
        if (form.pricePence <= 0) return "redirect:/admin/products/new";

        // Create product
        ProductEntity p = new ProductEntity(form.name.trim(), form.slug.trim(), form.pricePence, form.imageUrl.trim());
        p = productRepo.save(p);

        // Create variants S/M/L
        variantRepo.saveAll(List.of(
                new VariantEntity(p, "S", Math.max(0, form.stockS)),
                new VariantEntity(p, "M", Math.max(0, form.stockM)),
                new VariantEntity(p, "L", Math.max(0, form.stockL))
        ));

        return "redirect:/admin/products";
    }

    @GetMapping("/products/{id}/edit")
    public String editProduct(@PathVariable Long id, Model model) {
        ProductEntity p = productRepo.findById(id).orElse(null);
        if (p == null) return "redirect:/admin/products";

        List<VariantEntity> variants = variantRepo.findByProductOrderBySizeAsc(p);

        ProductForm form = new ProductForm();
        form.name = p.getName();
        form.slug = p.getSlug();
        form.imageUrl = p.getImageUrl();
        form.pricePence = p.getPricePence();

        for (VariantEntity v : variants) {
            switch (v.getSize()) {
                case "S" -> form.stockS = v.getStock();
                case "M" -> form.stockM = v.getStock();
                case "L" -> form.stockL = v.getStock();
            }
        }

        model.addAttribute("productId", id);
        model.addAttribute("form", form);
        return "admin-product-edit";
    }

    @PostMapping("/products/{id}/edit")
    public String updateProduct(@PathVariable Long id, @ModelAttribute("form") ProductForm form) {
        ProductEntity p = productRepo.findById(id).orElse(null);
        if (p == null) return "redirect:/admin/products";

        // Update product fields
        p.setName(form.name.trim());
        p.setSlug(form.slug.trim());
        p.setImageUrl(form.imageUrl.trim());
        p.setPricePence(form.pricePence);
        productRepo.save(p);

        // Update variants
        updateVariantStock(p, "S", form.stockS);
        updateVariantStock(p, "M", form.stockM);
        updateVariantStock(p, "L", form.stockL);

        return "redirect:/admin/products";
    }

    private void updateVariantStock(ProductEntity p, String size, int stock) {
        VariantEntity v = variantRepo.findByProductAndSize(p, size).orElse(null);
        if (v == null) {
            variantRepo.save(new VariantEntity(p, size, Math.max(0, stock)));
        } else {
            v.setStock(Math.max(0, stock));
            variantRepo.save(v);
        }
    }
    @PostMapping("/products/{id}/delete")
    public String deleteProduct(@PathVariable Long id) {
        ProductEntity p = productRepo.findById(id).orElse(null);
        if (p == null) return "redirect:/admin/products";

        // Soft delete: remove from store but keep record
        p.setActive(false);
        productRepo.save(p);

        return "redirect:/admin/products";
    }
}