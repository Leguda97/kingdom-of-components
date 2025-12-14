package cz.osu.opr3_backend.service;

import lombok.Getter;

import java.util.List;

@Getter
public class BuildValidationException extends RuntimeException {
    private final List<String> reasons;

    public BuildValidationException(List<String> reasons) {
        super("Build validation failed");
        this.reasons = reasons;
    }
}
