package cz.osu.opr3_backend.web.dto;

import cz.osu.opr3_backend.model.entity.Product;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record ProductCreateRequest(
        @NotBlank @Size(max = 64) String sku,
        @NotBlank String name,
        @NotNull Product.Category category,
        @NotNull @Positive BigDecimal price,
        @NotNull @Min(0) Integer stock,
        String spec
) {}
