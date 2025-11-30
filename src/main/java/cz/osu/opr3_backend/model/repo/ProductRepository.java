package cz.osu.opr3_backend.model.repo;

import cz.osu.opr3_backend.model.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
