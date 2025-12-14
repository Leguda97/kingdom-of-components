package cz.osu.opr3_backend.web.dto.order;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record OrderItemAddRequest(
        @NotNull Long productId,
        @NotNull @Min(1) Integer quantity
) {}
