package com.pinkarchive.backend.controller;

import com.pinkarchive.backend.controller.dto.ProductForm;
import com.pinkarchive.backend.db.ProductEntity;
import com.pinkarchive.backend.db.ProductRepository;
import com.pinkarchive.backend.db.VariantEntity;
import com.pinkarchive.backend.db.VariantRepository;
import org.springframework.dao.DataIntegrityViolationException;
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
        form.setStockS(10);
        form.setStockM(10);
        form.setStockL(10);
        model.addAttribute("form", form);
        return "admin-product-new";
    }

    @PostMapping("/products/new")
    public String createProduct(@ModelAttribute("form") ProductForm form) {

        if (form.getName() == null || form.getName().isBlank()) return "redirect:/admin/products/new";
        if (form.getSlug() == null || form.getSlug().isBlank()) return "redirect:/admin/products/new";
        if (form.getImageUrl() == null || form.getImageUrl().isBlank()) return "redirect:/admin/products/new";
        if (form.getPricePence() <= 0) return "redirect:/admin/products/new";

        try {
            ProductEntity p = new ProductEntity(
                    form.getName().trim(),
                    form.getSlug().trim(),
                    form.getPricePence(),
                    form.getImageUrl().trim()
            );
            p = productRepo.save(p);

            variantRepo.saveAll(List.of(
                    new VariantEntity(p, "S", Math.max(0, form.getStockS())),
                    new VariantEntity(p, "M", Math.max(0, form.getStockM())),
                    new VariantEntity(p, "L", Math.max(0, form.getStockL()))
            ));

            return "redirect:/admin/products";
        } catch (DataIntegrityViolationException ex) {
            return "redirect:/admin/products/new?error=slug";
        }
    }

    @GetMapping("/products/{id}/edit")
    public String editProduct(@PathVariable Long id, Model model) {
        ProductEntity p = productRepo.findById(id).orElse(null);
        if (p == null) return "redirect:/admin/products";

        List<VariantEntity> variants = variantRepo.findByProductOrderBySizeAsc(p);

        ProductForm form = new ProductForm();
        form.setName(p.getName());
        form.setSlug(p.getSlug());
        form.setImageUrl(p.getImageUrl());
        form.setPricePence(p.getPricePence());

        for (VariantEntity v : variants) {
            switch (v.getSize()) {
                case "S" -> form.setStockS(v.getStock());
                case "M" -> form.setStockM(v.getStock());
                case "L" -> form.setStockL(v.getStock());
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

        try {
            p.setName(form.getName().trim());
            p.setSlug(form.getSlug().trim());
            p.setImageUrl(form.getImageUrl().trim());
            p.setPricePence(form.getPricePence());
            productRepo.save(p);

            updateVariantStock(p, "S", form.getStockS());
            updateVariantStock(p, "M", form.getStockM());
            updateVariantStock(p, "L", form.getStockL());

            return "redirect:/admin/products";
        } catch (DataIntegrityViolationException ex) {
            return "redirect:/admin/products/" + id + "/edit?error=slug";
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

    private void updateVariantStock(ProductEntity p, String size, int stock) {
        VariantEntity v = variantRepo.findByProductAndSize(p, size).orElse(null);
        if (v == null) {
            variantRepo.save(new VariantEntity(p, size, Math.max(0, stock)));
        } else {
            v.setStock(Math.max(0, stock));
            variantRepo.save(v);
        }
    }
}