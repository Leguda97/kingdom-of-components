package cz.osu.opr3_backend.web;

import cz.osu.opr3_backend.model.entity.Order;
import cz.osu.opr3_backend.service.OrderService;
import cz.osu.opr3_backend.web.dto.order.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create() {
        return OrderResponse.of(orderService.create());
    }

    @GetMapping
    public List<OrderResponse> list(@RequestParam(required = false) Order.Status status) {
        return orderService.listMy().stream()
                .map(OrderResponse::of)
                .toList();
    }

    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable Long id) {
        return OrderResponse.of(orderService.getMy(id));
    }

    @PostMapping("/{id}/items")
    public OrderResponse addItem(@PathVariable Long id, @RequestBody @Valid OrderItemAddRequest req) {
        return OrderResponse.of(orderService.addItem(id, req));
    }

    @DeleteMapping("/{id}/items/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeItem(@PathVariable Long id, @PathVariable Long itemId) {
        orderService.removeItem(id, itemId);
    }
}
