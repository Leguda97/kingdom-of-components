package cz.osu.opr3_backend.service;

public class OrderStateException extends RuntimeException {
    public OrderStateException(String message) {
        super(message);
    }
}
