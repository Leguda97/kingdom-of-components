package cz.osu.opr3_backend.web;

import cz.osu.opr3_backend.service.BuildService;
import cz.osu.opr3_backend.web.dto.build.BuildCreateRequest;
import cz.osu.opr3_backend.web.dto.build.BuildItemAddRequest;
import cz.osu.opr3_backend.web.dto.build.BuildResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import cz.osu.opr3_backend.web.dto.order.OrderResponse;
import cz.osu.opr3_backend.web.dto.build.CompatibilityReportResponse;
import cz.osu.opr3_backend.web.dto.build.BuildValidationResponse;
import cz.osu.opr3_backend.web.dto.build.BuildSummaryResponse;

import java.util.List;

@RestController
@RequestMapping("/api/builds")
@RequiredArgsConstructor
public class BuildController {

    private final BuildService buildService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BuildResponse create(@RequestBody @Valid BuildCreateRequest req) {
        return BuildResponse.of(buildService.create(req));
    }

    @PostMapping("/{id}/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse checkout(@PathVariable Long id) {
        return OrderResponse.of(buildService.checkout(id));
    }

    @PostMapping("/{id}/items")
    public BuildResponse addItem(@PathVariable Long id, @RequestBody @Valid BuildItemAddRequest req) {
        return BuildResponse.of(buildService.addItem(id, req));
    }

    @GetMapping
    public List<BuildResponse> list() {
        return buildService.list().stream().map(BuildResponse::of).toList();
    }

    @GetMapping("/{id}")
    public BuildResponse get(@PathVariable Long id) {
        return BuildResponse.of(buildService.get(id));
    }

    @GetMapping("/{id}/compatibility")
    public CompatibilityReportResponse compatibility(@PathVariable Long id) {
        return buildService.checkCompatibility(id);
    }

    @GetMapping("/{id}/validate")
    public BuildValidationResponse validate(@PathVariable Long id) {
        return buildService.validateBuild(id);
    }

    @GetMapping("/{id}/summary")
    public BuildSummaryResponse summary(@PathVariable Long id) {
        return buildService.getBuildSummary(id);
    }

    @DeleteMapping("/{id}/items/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeItem(@PathVariable Long id, @PathVariable Long itemId) {
        buildService.removeItem(id, itemId);
    }
}
