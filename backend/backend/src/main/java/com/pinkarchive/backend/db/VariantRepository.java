package com.pinkarchive.backend.db;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VariantRepository extends JpaRepository<VariantEntity, Long> {
    List<VariantEntity> findByProductAndStockGreaterThanEqualOrderBySizeAsc(ProductEntity product, int stock);
    List<VariantEntity> findByProductOrderBySizeAsc(ProductEntity product);
    Optional<VariantEntity> findByProductAndSize(ProductEntity product, String size);
}