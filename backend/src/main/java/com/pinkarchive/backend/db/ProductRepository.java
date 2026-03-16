package com.pinkarchive.backend.db;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<ProductEntity, Long> {

    List<ProductEntity> findByActiveTrue();
    Optional<ProductEntity> findBySlugAndActiveTrue(String slug);

    List<ProductEntity> findDistinctByActiveTrueAndVariants_SizeAndVariants_StockGreaterThan(String size, int stock);
}