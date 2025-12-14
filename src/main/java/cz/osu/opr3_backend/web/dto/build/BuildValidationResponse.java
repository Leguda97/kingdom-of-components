package cz.osu.opr3_backend.web.dto.build;

import java.util.List;

public record BuildValidationResponse(
        Long buildId,
        boolean ok,
        List<String> reasons
) {}
