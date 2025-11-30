package cz.osu.opr3_backend.web.dto;

import cz.osu.opr3_backend.model.entity.Product;
import java.math.BigDecimal;

public record ProductResponse(
        Long id,
        String sku,
        String name,
        Product.Category category,
        BigDecimal price,
        Integer stock,
        String spec
) {
    public static ProductResponse of(Product p) {
        return new ProductResponse(
                p.getId(),
                p.getSku(),
                p.getName(),
                p.getCategory(),
                p.getPrice(),
                p.getStock(),
                p.getSpec()
        );
    }
}
