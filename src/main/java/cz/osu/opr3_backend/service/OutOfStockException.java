package cz.osu.opr3_backend.service;

import lombok.Getter;

@Getter
public class OutOfStockException extends RuntimeException {
    private final Long productId;
    private final int requested;
    private final int available;

    public OutOfStockException(Long productId, int requested, int available) {
        super("Not enough stock for product " + productId + " (requested " + requested + ", available " + available + ")");
        this.productId = productId;
        this.requested = requested;
        this.available = available;
    }
}
