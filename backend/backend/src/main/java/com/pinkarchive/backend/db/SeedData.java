package com.pinkarchive.backend.db;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SeedData implements CommandLineRunner {

    private final ProductRepository productRepo;
    private final VariantRepository variantRepo;

    public SeedData(ProductRepository productRepo, VariantRepository variantRepo) {
        this.productRepo = productRepo;
        this.variantRepo = variantRepo;
    }

    @Override
    public void run(String... args) {

        // Seed products once
        if (productRepo.count() == 0) {
            productRepo.saveAll(List.of(
                    new ProductEntity("Blush Matching Set", "blush-matching-set", 2999, "https://via.placeholder.com/600x800"),
                    new ProductEntity("Baby Pink Lounge Set", "baby-pink-lounge-set", 3499, "https://via.placeholder.com/600x800"),
                    new ProductEntity("Pastel Zip Set", "pastel-zip-set", 3999, "https://via.placeholder.com/600x800"),
                    new ProductEntity("Soft Pink Hoodie Set", "soft-pink-hoodie-set", 4299, "https://via.placeholder.com/600x800"),
                    new ProductEntity("Archive Rib Set", "archive-rib-set", 2799, "https://via.placeholder.com/600x800"),
                    new ProductEntity("Coquette Set", "coquette-set", 3199, "https://via.placeholder.com/600x800")
            ));
        }

        // Seed variants once (if variants empty)
        if (variantRepo.count() == 0) {
            List<ProductEntity> products = productRepo.findAll();

            for (ProductEntity p : products) {
                variantRepo.saveAll(List.of(
                        new VariantEntity(p, "S", 10),
                        new VariantEntity(p, "M", 10),
                        new VariantEntity(p, "L", 10)
                ));
            }
        }
    }
}
