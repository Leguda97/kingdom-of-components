package cz.osu.opr3_backend.service;

import cz.osu.opr3_backend.model.entity.Product;
import cz.osu.opr3_backend.model.repo.ProductRepository;
import cz.osu.opr3_backend.security.SecurityUtils;
import cz.osu.opr3_backend.web.dto.ProductCreateRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;

    public Product create(ProductCreateRequest req) {
        String actor = SecurityUtils.usernameOrAnonymous();

        Product p = Product.builder()
                .sku(req.sku())
                .name(req.name())
                .category(req.category())
                .price(req.price())
                .stock(req.stock())
                .spec(req.spec())
                .build();

        Product saved = productRepository.save(p);

        log.info("AUDIT PRODUCT_CREATE actor={} productId={} sku={} category={} price={} stock={}",
                actor, saved.getId(), saved.getSku(), saved.getCategory(), saved.getPrice(), saved.getStock());

        return saved;
    }

    public Product get(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product " + id + " not found"));
    }

    public Product update(Long id, ProductCreateRequest req) {
        String actor = SecurityUtils.usernameOrAnonymous();

        Product existing = get(id);

        String oldSku = existing.getSku();
        Integer oldStock = existing.getStock();

        existing.setSku(req.sku());
        existing.setName(req.name());
        existing.setCategory(req.category());
        existing.setPrice(req.price());
        existing.setStock(req.stock());
        existing.setSpec(req.spec());

        Product saved = productRepository.save(existing);

        log.info("AUDIT PRODUCT_UPDATE actor={} productId={} oldSku={} newSku={} oldStock={} newStock={}",
                actor, saved.getId(), oldSku, saved.getSku(), oldStock, saved.getStock());

        return saved;
    }

    public void delete(Long id) {
        String actor = SecurityUtils.usernameOrAnonymous();

        Product existing = get(id);
        productRepository.delete(existing);

        log.warn("AUDIT PRODUCT_DELETE actor={} productId={} sku={} name={}",
                actor, existing.getId(), existing.getSku(), existing.getName());
    }

    public Product updateStock(Long id, Integer stock) {
        String actor = SecurityUtils.usernameOrAnonymous();

        Product p = get(id);
        Integer old = p.getStock();

        p.setStock(stock);
        Product saved = productRepository.save(p);

        log.info("AUDIT PRODUCT_STOCK_UPDATE actor={} productId={} sku={} oldStock={} newStock={}",
                actor, saved.getId(), saved.getSku(), old, stock);

        return saved;
    }

    public List<Product> findAll(Product.Category category, String q) {
        if (category != null && q != null && !q.isBlank()) {
            return productRepository.findByCategoryAndNameContainingIgnoreCase(category, q);
        }
        if (category != null) {
            return productRepository.findByCategory(category);
        }
        if (q != null && !q.isBlank()) {
            return productRepository.findByNameContainingIgnoreCase(q);
        }
        return productRepository.findAll();
    }

    public List<Product> list() {
        return productRepository.findAll();
    }
}
