package cz.osu.opr3_backend.web.dto.build;

import jakarta.validation.constraints.NotBlank;

public record BuildCreateRequest(
        @NotBlank String name
) {}
