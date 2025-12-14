package cz.osu.opr3_backend.web;

import cz.osu.opr3_backend.model.entity.Order;
import cz.osu.opr3_backend.service.OrderService;
import cz.osu.opr3_backend.web.dto.order.OrderResponse;
import cz.osu.opr3_backend.web.dto.order.OrderStatusUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping
    public List<OrderResponse> listAll() {
        return orderService.listAllAdmin().stream().map(OrderResponse::of).toList();
    }

    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable Long id) {
        return OrderResponse.of(orderService.getAnyAdmin(id));
    }

    @PutMapping("/{id}/status")
    public OrderResponse updateStatus(@PathVariable Long id, @RequestBody @Valid OrderStatusUpdateRequest req) {
        return OrderResponse.of(orderService.updateStatusAdmin(id, req.status()));

    }
}
