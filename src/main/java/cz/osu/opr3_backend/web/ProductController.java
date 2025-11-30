package cz.osu.opr3_backend.web;

import cz.osu.opr3_backend.model.entity.Product;
import cz.osu.opr3_backend.service.ProductService;
import cz.osu.opr3_backend.web.dto.ProductCreateRequest;
import cz.osu.opr3_backend.web.dto.ProductResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // POST /api/products  – vytvoření produktu
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@RequestBody @Valid ProductCreateRequest req) {
        Product p = productService.create(req);
        return ProductResponse.of(p);
    }

    // GET /api/products/{id} – detail produktu podle ID
    @GetMapping("/{id}")
    public ProductResponse get(@PathVariable Long id) {
        Product p = productService.get(id);
        return ProductResponse.of(p);
    }

    // GET /api/products – seznam všech produktů
    @GetMapping
    public List<ProductResponse> list() {
        return productService.list().stream()
                .map(ProductResponse::of)
                .toList();
    }

    // PUT /api/products/{id} – úprava existujícího produktu
    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable Long id,
                                  @RequestBody @Valid ProductCreateRequest req) {
        Product updated = productService.update(id, req);
        return ProductResponse.of(updated);
    }

    // DELETE /api/products/{id} – smazání produktu
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        productService.delete(id);
    }

}
