package cz.osu.opr3_backend.web.dto.build;

import java.util.List;

public record CompatibilityReportResponse(
        Long buildId,
        boolean compatible,
        List<String> errors,
        List<String> warnings,
        Integer estimatedLoadW,
        Integer psuWattageW,
        String cpuSocket,
        String mbSocket
) {}
