package cz.osu.opr3_backend.web.dto.build;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record BuildItemAddRequest(
        @NotNull Long productId,
        @NotNull @Min(1) Integer quantity
) {}
