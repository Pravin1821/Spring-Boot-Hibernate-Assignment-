package com.example.productinventory.service;

import com.example.productinventory.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

public interface ProductService {
    Product addProduct(Product product);
    Product updateProduct(Long id, Product product);
    void deleteProduct(Long id);
    Product getProductById(Long id);
    Page<Product> getProducts(String category, BigDecimal minPrice, BigDecimal maxPrice, Integer maxQuantity, Pageable pageable);
}
