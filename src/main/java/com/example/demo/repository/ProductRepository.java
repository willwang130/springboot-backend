package com.example.demo.repository;
import com.example.demo.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

// JpaRepository<Product, Long> 让Spring自动生产增删改查（CRUD）的方法
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByNameContainingAndPriceGreaterThanEqual(String name, double price);

    // 找到 Product 表最新创建的 id
    @Query(value = "SELECT id FROM product ORDER BY created_at DESC LIMIT 1", nativeQuery = true)
    Optional<Long> findFirstValidProductId();

    @Query("SELECT p.id FROM Product p")
    List<Long> findAllProductIds();
}
