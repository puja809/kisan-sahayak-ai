package com.farmer.user.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Crop entity representing a crop grown by a user.
 */
@Entity
@Table(name = "user_crops")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Crop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "crop_name", nullable = false, length = 100)
    private String cropName;

    @Column(name = "crop_variety", length = 100)
    private String cropVariety;

    @Column(name = "sowing_date", nullable = false)
    private LocalDate sowingDate;

    @Column(name = "expected_harvest_date")
    private LocalDate expectedHarvestDate;

    @Column(name = "actual_harvest_date")
    private LocalDate actualHarvestDate;

    @Column(name = "area_acres", nullable = false)
    private Double areaAcres;

    @Enumerated(EnumType.STRING)
    @Column(name = "season")
    private Season season;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    private CropStatus status = CropStatus.SOWN;

    @Column(name = "seed_cost")
    private Double seedCost;

    @Column(name = "fertilizer_cost")
    private Double fertilizerCost;

    @Column(name = "pesticide_cost")
    private Double pesticideCost;

    @Column(name = "labor_cost")
    private Double laborCost;

    @Column(name = "other_cost")
    private Double otherCost;

    @Column(name = "total_input_cost")
    private Double totalInputCost;

    @Column(name = "total_yield_quintals")
    private Double totalYieldQuintals;

    @Column(name = "quality_grade", length = 20)
    private String qualityGrade;

    @Column(name = "selling_price_per_quintal")
    private Double sellingPricePerQuintal;

    @Column(name = "mandi_name", length = 100)
    private String mandiName;

    @Column(name = "total_revenue")
    private Double totalRevenue;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    public enum Season {
        KHARIF, RABI, ZAID
    }

    public enum CropStatus {
        SOWN, GROWING, HARVESTED, FAILED
    }

    @PrePersist
    @PreUpdate
    public void onSaveOrUpdate() {
        this.updatedAt = LocalDateTime.now();
        Double total = 0.0;
        if (seedCost != null)
            total += seedCost;
        if (fertilizerCost != null)
            total += fertilizerCost;
        if (pesticideCost != null)
            total += pesticideCost;
        if (laborCost != null)
            total += laborCost;
        if (otherCost != null)
            total += otherCost;
        this.totalInputCost = total;

        if (totalYieldQuintals != null && sellingPricePerQuintal != null) {
            this.totalRevenue = totalYieldQuintals * sellingPricePerQuintal;
        }
    }
}