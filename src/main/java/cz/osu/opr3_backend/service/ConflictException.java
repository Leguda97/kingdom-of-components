package cz.osu.opr3_backend.service;

public class ConflictException extends RuntimeException {
    public ConflictException(String message) { super(message); }
}
