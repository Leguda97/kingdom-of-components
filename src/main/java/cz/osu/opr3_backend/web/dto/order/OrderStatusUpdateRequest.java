package cz.osu.opr3_backend.web.dto.order;

import cz.osu.opr3_backend.model.entity.Order;
import jakarta.validation.constraints.NotNull;

public record OrderStatusUpdateRequest(
        @NotNull Order.Status status
) {}
