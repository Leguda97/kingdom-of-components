package cz.osu.opr3_backend.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ProductStockUpdateRequest(
        @NotNull @Min(0) Integer stock
) {}
