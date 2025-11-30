package cz.osu.opr3_backend.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(
        name = "product",
        indexes = @Index(name = "ix_product_category", columnList = "category"),
        uniqueConstraints = @UniqueConstraint(name = "ux_product_sku", columnNames = "sku")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String sku;   // skladové číslo

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Category category;

    @Column(nullable = false)
    private BigDecimal price;

    // Parametry komponenty (socket, TDP, délka GPU, ...)
    @Column(columnDefinition = "text")
    private String spec;

    @Column(nullable = false)
    private Integer stock;

    public enum Category {
        CPU, MB, RAM, GPU, CASE, PSU, STORAGE, COOLER, OTHER
    }
}
