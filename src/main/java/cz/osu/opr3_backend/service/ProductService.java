package cz.osu.opr3_backend.service;

import cz.osu.opr3_backend.model.entity.Product;
import cz.osu.opr3_backend.model.repo.ProductRepository;
import cz.osu.opr3_backend.web.dto.ProductCreateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public Product create(ProductCreateRequest req) {
        Product p = Product.builder()
                .sku(req.sku())
                .name(req.name())
                .category(req.category())
                .price(req.price())
                .stock(req.stock())
                .spec(req.spec())
                .build();
        return productRepository.save(p);
    }

    public Product get(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product " + id + " not found"));
    }

    public Product update(Long id, ProductCreateRequest req) {
        // Najdeme existující produkt, nebo vyhodíme 404
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product " + id + " not found"));

        // Aktualizujeme jeho vlastnosti
        existing.setSku(req.sku());
        existing.setName(req.name());
        existing.setCategory(req.category());
        existing.setPrice(req.price());
        existing.setStock(req.stock());
        existing.setSpec(req.spec());

        // Uložíme změny
        return productRepository.save(existing);
    }

    public void delete(Long id) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product " + id + " not found"));

        productRepository.delete(existing);
    }

    public List<Product> list() {
        return productRepository.findAll();
    }
}
