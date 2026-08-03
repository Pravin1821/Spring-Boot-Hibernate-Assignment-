package com.example.productinventory.service.impl;

import com.example.productinventory.exception.DuplicateProductNameException;
import com.example.productinventory.exception.ProductNotFoundException;
import com.example.productinventory.model.Product;
import com.example.productinventory.repository.ProductRepository;
import com.example.productinventory.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    @Autowired
    public ProductServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public Product addProduct(Product product) {
        if (productRepository.existsByName(product.getName())) {
            throw new DuplicateProductNameException("Product with name '" + product.getName() + "' already exists.");
        }
        return productRepository.save(product);
    }

    @Override
    public Product updateProduct(Long id, Product updatedProduct) {
        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product with ID " + id + " not found."));

        if (productRepository.existsByNameAndIdNot(updatedProduct.getName(), id)) {
            throw new DuplicateProductNameException("Product with name '" + updatedProduct.getName() + "' already exists.");
        }

        existingProduct.setName(updatedProduct.getName());
        existingProduct.setCategory(updatedProduct.getCategory());
        existingProduct.setPrice(updatedProduct.getPrice());
        existingProduct.setQuantity(updatedProduct.getQuantity());
        existingProduct.setSupplierName(updatedProduct.getSupplierName());

        return productRepository.save(existingProduct);
    }

    @Override
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ProductNotFoundException("Product with ID " + id + " not found.");
        }
        productRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product with ID " + id + " not found."));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Product> getProducts(String category, BigDecimal minPrice, BigDecimal maxPrice, Integer maxQuantity, Pageable pageable) {
        Specification<Product> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (category != null && !category.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("category"), category));
            }

            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
            }

            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
            }

            if (maxQuantity != null) {
                predicates.add(cb.lessThan(root.get("quantity"), maxQuantity));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return productRepository.findAll(spec, pageable);
    }
}
