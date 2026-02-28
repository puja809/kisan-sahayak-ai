package com.farmer.yield.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Entity for storing yield calculation data from ultimate_calculator_app.json
 * Contains commodity-specific yield calculation parameters
 */
@Entity
@Table(name = "yield_calculators", indexes = {
    @Index(name = "idx_yield_calc_commodity", columnList = "commodity"),
    @Index(name = "idx_yield_calc_active", columnList = "is_active")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class YieldCalculator {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "commodity", nullable = false, unique = true, length = 100)
    private String commodity;

    @Column(name = "min_price_kg", nullable = false)
    private Double minPricePerKg;

    @Column(name = "avg_price_kg", nullable = false)
    private Double avgPricePerKg;

    @Column(name = "max_price_kg", nullable = false)
    private Double maxPricePerKg;

    @Column(name = "base_yield_per_hectare", nullable = false)
    private Double baseYieldPerHectare;

    @Column(name = "yield_variance_percent")
    private Double yieldVariancePercent;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
