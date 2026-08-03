package com.example.productinventory.repository;

import com.example.productinventory.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    
    boolean existsByName(String name);
    
    boolean existsByNameAndIdNot(String name, Long id);
}
