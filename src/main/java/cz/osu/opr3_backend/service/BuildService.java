package cz.osu.opr3_backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.osu.opr3_backend.model.entity.*;
import cz.osu.opr3_backend.model.repo.*;
import cz.osu.opr3_backend.security.CurrentUser;
import cz.osu.opr3_backend.security.SecurityUtils;
import cz.osu.opr3_backend.web.dto.build.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BuildService {

    private static final Logger log = LoggerFactory.getLogger(BuildService.class);

    private final BuildRepository buildRepository;
    private final BuildItemRepository buildItemRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    private static final int MAX_CPU = 1;
    private static final int MAX_GPU = 1;
    private static final int MAX_PSU = 1;
    private static final int MAX_CASE = 1;
    private static final int MAX_RAM = 4;

    // ----------------------------
    // Helpers
    // ----------------------------

    private User currentUserEntity() {
        String username = CurrentUser.username();
        if (username == null) throw new UnauthorizedException("Not authenticated");

        return userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    private Build getMyBuild(Long id) {
        String username = CurrentUser.username();
        if (username == null) throw new UnauthorizedException("Not authenticated");

        return buildRepository.findByIdAndOwner_Username(id, username)
                .orElseThrow(() -> new NotFoundException("Build " + id + " not found"));
    }

    // ----------------------------
    // CRUD
    // ----------------------------

    public Build create(BuildCreateRequest req) {
        String actor = SecurityUtils.usernameOrAnonymous();
        User owner = currentUserEntity();

        Build b = Build.builder()
                .name(req.name())
                .owner(owner)
                .build();

        Build saved = buildRepository.save(b);

        log.info("AUDIT BUILD_CREATE actor={} buildId={} owner={} name={}",
                actor, saved.getId(), owner.getUsername(), saved.getName());

        return saved;
    }

    public List<Build> list() {
        String actor = SecurityUtils.usernameOrAnonymous();
        String username = CurrentUser.username();
        if (username == null) throw new UnauthorizedException("Not authenticated");

        List<Build> res = buildRepository.findAllByOwner_UsernameOrderByCreatedAtDesc(username);

        log.info("AUDIT BUILD_LIST actor={} owner={} count={}", actor, username, res.size());
        return res;
    }

    public Build get(Long id) {
        String actor = SecurityUtils.usernameOrAnonymous();
        Build b = getMyBuild(id);
        log.info("AUDIT BUILD_GET actor={} buildId={} owner={}", actor, b.getId(), b.getOwner().getUsername());
        return b;
    }

    // ----------------------------
    // Items
    // ----------------------------

    @Transactional
    public Build addItem(Long buildId, BuildItemAddRequest req) {
        String actor = SecurityUtils.usernameOrAnonymous();
        Build build = getMyBuild(buildId);

        if (req.quantity() <= 0) {
            log.warn("AUDIT BUILD_ADD_ITEM_DENIED actor={} buildId={} reason=qty<=0 qty={}",
                    actor, buildId, req.quantity());
            throw new IllegalArgumentException("Quantity must be > 0");
        }

        Product product = productRepository.findById(req.productId())
                .orElseThrow(() -> new NotFoundException("Product " + req.productId() + " not found"));

        for (BuildItem bi : build.getItems()) {
            if (bi.getProduct().getId().equals(product.getId())) {
                int oldQty = bi.getQuantity();
                int newQty = oldQty + req.quantity();
                bi.setQuantity(newQty);

                // limity (CPU/GPU/PSU/CASE max 1, RAM max 4)
                try {
                    enforceCategoryLimitsOrThrow(build);
                } catch (BuildValidationException e) {
                    log.warn("AUDIT BUILD_ADD_ITEM_DENIED actor={} buildId={} productId={} reason={}",
                            actor, buildId, product.getId(), e.getReasons());
                    throw e;
                }

                Build saved = buildRepository.save(build);

                log.info("AUDIT BUILD_ADD_ITEM_MERGE actor={} buildId={} owner={} productId={} sku={} oldQty={} addQty={} newQty={}",
                        actor, buildId, build.getOwner().getUsername(), product.getId(), product.getSku(), oldQty, req.quantity(), newQty);

                return saved;
            }
        }

        // Nový Item
        BuildItem item = BuildItem.builder()
                .build(build)
                .product(product)
                .quantity(req.quantity())
                .build();

        build.getItems().add(item);

        try {
            enforceCategoryLimitsOrThrow(build);
            Build saved = buildRepository.save(build);

            log.info("AUDIT BUILD_ADD_ITEM_NEW actor={} buildId={} owner={} productId={} sku={} qty={}",
                    actor, buildId, build.getOwner().getUsername(), product.getId(), product.getSku(), req.quantity());

            return saved;

        } catch (BuildValidationException e) {
            log.warn("AUDIT BUILD_ADD_ITEM_DENIED actor={} buildId={} productId={} reason={}",
                    actor, buildId, product.getId(), e.getReasons());
            throw e;

        } catch (DataIntegrityViolationException e) {
            log.warn("AUDIT BUILD_ADD_ITEM_FAIL actor={} buildId={} productId={} reason={}",
                    actor, buildId, product.getId(), e.getMessage());
            throw e;
        }
    }

    @Transactional
    public void removeItem(Long buildId, Long itemId) {
        String actor = SecurityUtils.usernameOrAnonymous();
        Build build = getMyBuild(buildId);

        BuildItem item = build.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("BuildItem " + itemId + " not found in build " + buildId));

        build.getItems().remove(item);
        buildItemRepository.delete(item);
        buildRepository.save(build);

        log.info("AUDIT BUILD_REMOVE_ITEM actor={} buildId={} owner={} itemId={} productId={} sku={}",
                actor, buildId, build.getOwner().getUsername(), itemId, item.getProduct().getId(), item.getProduct().getSku());
    }

    // ----------------------------
    // Checkout
    // ----------------------------

    @Transactional
    public Order checkout(Long buildId) {
        String actor = SecurityUtils.usernameOrAnonymous();

        // validate throws if blocked
        validateBuildOrThrow(buildId);

        Build build = getMyBuild(buildId);
        User owner = currentUserEntity();

        if (build.getItems().isEmpty()) {
            log.warn("AUDIT BUILD_CHECKOUT_DENIED actor={} buildId={} owner={} reason=empty_build",
                    actor, buildId, owner.getUsername());
            throw new IllegalStateException("Build " + buildId + " is empty");
        }

        // Kontrola skladu
        for (BuildItem bi : build.getItems()) {
            Product p = bi.getProduct();
            int requested = bi.getQuantity();
            int available = (p.getStock() == null) ? 0 : p.getStock();

            if (requested > available) {
                log.warn("AUDIT BUILD_CHECKOUT_DENIED actor={} buildId={} owner={} reason=out_of_stock productId={} sku={} requested={} available={}",
                        actor, buildId, owner.getUsername(), p.getId(), p.getSku(), requested, available);
                throw new OutOfStockException(p.getId(), requested, available);
            }
        }

        Order order = Order.builder()
                .owner(owner)
                .build();

        if (order.getItems() == null) order.setItems(new ArrayList<>());

        for (BuildItem bi : build.getItems()) {
            Product p = bi.getProduct();

            OrderItem oi = OrderItem.builder()
                    .order(order)
                    .product(p)
                    .quantity(bi.getQuantity())
                    .unitPrice(p.getPrice())
                    .build();

            order.getItems().add(oi);

            // odebreme ze skladu
            int current = (p.getStock() == null) ? 0 : p.getStock();
            p.setStock(current - bi.getQuantity());
        }

        BigDecimal total = BigDecimal.ZERO;
        for (OrderItem oi : order.getItems()) {
            total = total.add(oi.getUnitPrice().multiply(BigDecimal.valueOf(oi.getQuantity())));
        }
        order.setTotalPrice(total);

        Order saved = orderRepository.save(order);

        log.info("AUDIT BUILD_CHECKOUT_OK actor={} buildId={} owner={} orderId={} totalPrice={}",
                actor, buildId, owner.getUsername(), saved.getId(), saved.getTotalPrice());

        return saved;
    }

    // ----------------------------
    // Compatibility / Validation / Summary
    // ----------------------------

    public CompatibilityReportResponse checkCompatibility(Long buildId) {
        Build build = getMyBuild(buildId);

        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        BuildItem cpuItem = findFirstByCategory(build, Product.Category.CPU);
        BuildItem mbItem  = findFirstByCategory(build, Product.Category.MB);
        BuildItem psuItem = findFirstByCategory(build, Product.Category.PSU);

        String cpuSocket = null;
        String mbSocket = null;

        Integer cpuTdp = null;
        int gpuTdpSum = 0;
        Integer psuWattage = null;

        if (cpuItem != null) {
            cpuSocket = readStringSpec(cpuItem.getProduct().getSpec(), "socket", warnings, "CPU");
            cpuTdp = readIntSpec(cpuItem.getProduct().getSpec(), "tdp", warnings, "CPU");
            if (cpuTdp != null) cpuTdp = cpuTdp * cpuItem.getQuantity();
        } else {
            warnings.add("No CPU in build");
        }

        if (mbItem != null) {
            mbSocket = readStringSpec(mbItem.getProduct().getSpec(), "socket", warnings, "MB");
        } else {
            warnings.add("No motherboard in build");
        }

        if (cpuSocket != null && mbSocket != null && !cpuSocket.equalsIgnoreCase(mbSocket)) {
            errors.add("CPU socket " + cpuSocket + " does not match MB socket " + mbSocket);
        } else if (cpuSocket == null && cpuItem != null) {
            warnings.add("CPU socket is missing in spec");
        } else if (mbSocket == null && mbItem != null) {
            warnings.add("MB socket is missing in spec");
        }

        if (psuItem != null) {
            psuWattage = readIntSpec(psuItem.getProduct().getSpec(), "wattage", warnings, "PSU");
            if (psuWattage == null) warnings.add("PSU wattage is missing in spec");
        } else {
            warnings.add("No PSU in build");
        }

        for (BuildItem bi : build.getItems()) {
            if (bi.getProduct().getCategory() == Product.Category.GPU) {
                Integer tdp = readIntSpec(bi.getProduct().getSpec(), "tdp", warnings, "GPU");
                if (tdp != null) gpuTdpSum += (tdp * bi.getQuantity());
                else warnings.add("GPU TDP is missing in spec (productId " + bi.getProduct().getId() + ")");
            }
        }

        Integer estimated = null;
        int reserve = 150;

        if (cpuTdp != null || gpuTdpSum > 0) {
            int cpuPart = (cpuTdp != null) ? cpuTdp : 0;
            estimated = cpuPart + gpuTdpSum + reserve;
        } else {
            warnings.add("Cannot estimate load (missing CPU/GPU TDP)");
        }

        if (estimated != null && psuWattage != null) {
            if (psuWattage < estimated) {
                errors.add("PSU wattage " + psuWattage + "W is below estimated load " + estimated + "W");
            } else if (psuWattage < (int) Math.ceil(estimated * 1.2)) {
                warnings.add("PSU wattage " + psuWattage + "W has low headroom for estimated load " + estimated + "W");
            }
        }

        boolean compatible = errors.isEmpty();

        return new CompatibilityReportResponse(
                build.getId(),
                compatible,
                errors,
                warnings,
                estimated,
                psuWattage,
                cpuSocket,
                mbSocket
        );
    }

    public BuildValidationResponse validateBuild(Long buildId) {
        Build build = get(buildId);

        List<String> reasons = new ArrayList<>();

        boolean hasCpu  = totalQtyByCategory(build, Product.Category.CPU)  > 0;
        boolean hasMb   = totalQtyByCategory(build, Product.Category.MB)   > 0;
        boolean hasPsu  = totalQtyByCategory(build, Product.Category.PSU)  > 0;
        boolean hasRam  = totalQtyByCategory(build, Product.Category.RAM)  > 0;
        boolean hasCase = totalQtyByCategory(build, Product.Category.CASE) > 0;

        if (!hasCpu)  reasons.add("Missing CPU");
        if (!hasMb)   reasons.add("Missing MB");
        if (!hasPsu)  reasons.add("Missing PSU");
        if (!hasRam)  reasons.add("Missing RAM");
        if (!hasCase) reasons.add("Missing CASE");

        int cpu = totalQtyByCategory(build, Product.Category.CPU);
        int gpu = totalQtyByCategory(build, Product.Category.GPU);
        int psu = totalQtyByCategory(build, Product.Category.PSU);
        int pcCase = totalQtyByCategory(build, Product.Category.CASE);
        int ram = totalQtyByCategory(build, Product.Category.RAM);

        if (cpu > MAX_CPU) reasons.add("CPU can be only once (max " + MAX_CPU + ")");
        if (gpu > MAX_GPU) reasons.add("GPU can be only once (max " + MAX_GPU + ")");
        if (psu > MAX_PSU) reasons.add("PSU can be only once (max " + MAX_PSU + ")");
        if (pcCase > MAX_CASE) reasons.add("Case can be only once (max " + MAX_CASE + ")");
        if (ram > MAX_RAM) reasons.add("RAM exceeds limit (max " + MAX_RAM + " sticks)");

        CompatibilityReportResponse report = checkCompatibility(buildId);
        reasons.addAll(report.errors());

        for (BuildItem bi : build.getItems()) {
            Product p = bi.getProduct();
            int requested = bi.getQuantity();
            int available = (p.getStock() == null) ? 0 : p.getStock();

            if (requested > available) {
                reasons.add("Not enough stock for " + p.getSku()
                        + " (requested " + requested + ", available " + available + ")");
            }
        }

        boolean ok = reasons.isEmpty();
        return new BuildValidationResponse(build.getId(), ok, reasons);
    }

    public void validateBuildOrThrow(Long buildId) {
        String actor = SecurityUtils.usernameOrAnonymous();
        BuildValidationResponse res = validateBuild(buildId);

        if (!res.ok()) {
            log.warn("AUDIT BUILD_VALIDATE_FAIL actor={} buildId={} reasons={}", actor, buildId, res.reasons());
            throw new BuildValidationException(res.reasons());
        }

        log.info("AUDIT BUILD_VALIDATE_OK actor={} buildId={}", actor, buildId);
    }

    public BuildSummaryResponse getBuildSummary(Long buildId) {
        Build build = getMyBuild(buildId);

        CompatibilityReportResponse compat = checkCompatibility(buildId);
        BuildValidationResponse validation = validateBuild(buildId);

        int distinctItemsCount = build.getItems().size();
        int totalQuantity = build.getItems().stream().mapToInt(BuildItem::getQuantity).sum();

        List<String> filteredWarnings = compat.warnings().stream()
                .filter(w -> !w.equals("No motherboard in build"))
                .filter(w -> !w.equals("No PSU in build"))
                .toList();

        return new BuildSummaryResponse(
                build.getId(),
                build.getName(),
                distinctItemsCount,
                totalQuantity,
                compat.compatible(),
                validation.ok(),
                compat.estimatedLoadW() != null ? compat.estimatedLoadW() : 0,
                compat.psuWattageW(),
                validation.reasons(),
                filteredWarnings
        );
    }

    // ----------------------------
    // Small helpers
    // ----------------------------

    private BuildItem findFirstByCategory(Build build, Product.Category category) {
        return build.getItems().stream()
                .filter(i -> i.getProduct().getCategory() == category)
                .findFirst()
                .orElse(null);
    }

    private String readStringSpec(String spec, String field, List<String> warnings, String label) {
        try {
            if (spec == null || spec.isBlank()) return null;
            JsonNode node = objectMapper.readTree(spec);
            JsonNode v = node.get(field);
            if (v == null || v.isNull()) return null;
            return v.asText();
        } catch (Exception e) {
            warnings.add(label + " spec is not valid JSON");
            return null;
        }
    }

    private Integer readIntSpec(String spec, String field, List<String> warnings, String label) {
        try {
            if (spec == null || spec.isBlank()) return null;
            JsonNode node = objectMapper.readTree(spec);
            JsonNode v = node.get(field);
            if (v == null || v.isNull()) return null;
            return v.asInt();
        } catch (Exception e) {
            warnings.add(label + " spec is not valid JSON");
            return null;
        }
    }

    private int totalQtyByCategory(Build build, Product.Category category) {
        return build.getItems().stream()
                .filter(i -> i.getProduct().getCategory() == category)
                .mapToInt(BuildItem::getQuantity)
                .sum();
    }

    private void enforceCategoryLimitsOrThrow(Build build) {
        List<String> reasons = new ArrayList<>();

        int cpu = totalQtyByCategory(build, Product.Category.CPU);
        int gpu = totalQtyByCategory(build, Product.Category.GPU);
        int psu = totalQtyByCategory(build, Product.Category.PSU);
        int pcCase = totalQtyByCategory(build, Product.Category.CASE);
        int ram = totalQtyByCategory(build, Product.Category.RAM);

        if (cpu > MAX_CPU) reasons.add("CPU can be only once (max " + MAX_CPU + ")");
        if (gpu > MAX_GPU) reasons.add("GPU can be only once (max " + MAX_GPU + ")");
        if (psu > MAX_PSU) reasons.add("PSU can be only once (max " + MAX_PSU + ")");
        if (pcCase > MAX_CASE) reasons.add("Case can be only once (max " + MAX_CASE + ")");
        if (ram > MAX_RAM) reasons.add("RAM exceeds limit (max " + MAX_RAM + " sticks)");

        if (!reasons.isEmpty()) {
            throw new BuildValidationException(reasons);
        }
    }
}
