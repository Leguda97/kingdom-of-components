package cz.osu.opr3_backend.web.dto.build;

import java.util.List;

public record BuildSummaryResponse(
        Long id,
        String name,
        int distinctItemsCount,
        int totalQuantity,
        boolean compatible,
        boolean readyForCheckout,
        int estimatedLoadW,
        Integer psuWattageW,
        List<String> errors,
        List<String> warnings
) {}
