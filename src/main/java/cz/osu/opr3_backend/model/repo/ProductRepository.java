package cz.osu.opr3_backend.model.repo;

import cz.osu.opr3_backend.model.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByCategory(Product.Category category);

    List<Product> findByNameContainingIgnoreCase(String q);

    List<Product> findByCategoryAndNameContainingIgnoreCase(
            Product.Category category,
            String q
    );
}
