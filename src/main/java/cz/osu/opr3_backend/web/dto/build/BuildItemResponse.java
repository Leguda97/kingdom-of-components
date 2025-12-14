package cz.osu.opr3_backend.web.dto.build;

import cz.osu.opr3_backend.model.entity.BuildItem;
import cz.osu.opr3_backend.web.dto.ProductResponse;

public record BuildItemResponse(
        Long id,
        Integer quantity,
        ProductResponse product
) {
    public static BuildItemResponse of(BuildItem bi) {
        return new BuildItemResponse(
                bi.getId(),
                bi.getQuantity(),
                ProductResponse.of(bi.getProduct())
        );
    }
}
