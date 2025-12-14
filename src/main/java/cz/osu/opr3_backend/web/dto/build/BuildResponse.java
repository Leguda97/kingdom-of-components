package cz.osu.opr3_backend.web.dto.build;

import cz.osu.opr3_backend.model.entity.Build;

import java.time.Instant;
import java.util.List;

public record BuildResponse(
        Long id,
        String name,
        Instant createdAt,
        List<BuildItemResponse> items
) {
    public static BuildResponse of(Build b) {
        return new BuildResponse(
                b.getId(),
                b.getName(),
                b.getCreatedAt(),
                b.getItems().stream().map(BuildItemResponse::of).toList()
        );
    }
}
