package cz.osu.opr3_backend.web.dto.order;

import cz.osu.opr3_backend.model.entity.Order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        Long id,
        Order.Status status,
        Instant createdAt,
        BigDecimal totalPrice,
        List<OrderItemResponse> items
) {
    public static OrderResponse of(Order o) {
        return new OrderResponse(
                o.getId(),
                o.getStatus(),
                o.getCreatedAt(),
                o.getTotalPrice(),
                o.getItems().stream().map(OrderItemResponse::of).toList()
        );
    }
}
