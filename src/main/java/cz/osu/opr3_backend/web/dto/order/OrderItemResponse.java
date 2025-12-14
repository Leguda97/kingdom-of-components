package cz.osu.opr3_backend.web.dto.order;

import cz.osu.opr3_backend.model.entity.OrderItem;
import cz.osu.opr3_backend.web.dto.ProductResponse;

import java.math.BigDecimal;

public record OrderItemResponse(
        Long id,
        Integer quantity,
        BigDecimal unitPrice,
        ProductResponse product
) {
    public static OrderItemResponse of(OrderItem oi) {
        return new OrderItemResponse(
                oi.getId(),
                oi.getQuantity(),
                oi.getUnitPrice(),
                ProductResponse.of(oi.getProduct())
        );
    }
}
