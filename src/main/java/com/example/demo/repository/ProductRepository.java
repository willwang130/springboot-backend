package com.example.demo.repository;
import com.example.demo.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// JpaRepository<Product, Long> 让Spring自动生产增删改查（CRUD）的方法
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByNameContainingAndPriceGreaterThanEqual(String name, double price);

}
