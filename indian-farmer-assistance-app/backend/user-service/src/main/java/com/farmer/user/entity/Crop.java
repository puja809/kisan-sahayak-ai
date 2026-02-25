package com.farmer.user.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Crop entity representing a crop planted on a farm.
 * Maps to the crops table with session-specific table prefix for data isolation.
 * 
 * Requirements: 11A.4, 11A.5, 11A.6
 */
@Entity
@Table(name = "sess_c05a946fe_crops")
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
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

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

    @Column(name = "area_acres", nullable = false, precision = 10, scale = 2)
    private Double areaAcres;

    @Enumerated(EnumType.STRING)
    @Column(name = "season")
    private Season season;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    private CropStatus status = CropStatus.SOWN;

    @Column(name = "seed_cost", precision = 10, scale = 2)
    private BigDecimal seedCost;

    @Column(name = "fertilizer_cost", precision = 10, scale = 2)
    private BigDecimal fertilizerCost;

    @Column(name = "pesticide_cost", precision = 10, scale = 2)
    private BigDecimal pesticideCost;

    @Column(name = "labor_cost", precision = 10, scale = 2)
    private BigDecimal laborCost;

    @Column(name = "other_cost", precision = 10, scale = 2)
    private BigDecimal otherCost;

    @Column(name = "total_input_cost", precision = 10, scale = 2)
    private BigDecimal totalInputCost;

    @Column(name = "total_yield_quintals", precision = 10, scale = 2)
    private BigDecimal totalYieldQuintals;

    @Column(name = "quality_grade", length = 20)
    private String qualityGrade;

    @Column(name = "selling_price_per_quintal", precision = 10, scale = 2)
    private BigDecimal sellingPricePerQuintal;

    @Column(name = "mandi_name", length = 100)
    private String mandiName;

    @Column(name = "total_revenue", precision = 12, scale = 2)
    private BigDecimal totalRevenue;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "crop", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<FertilizerApplication> fertilizerApplications = new ArrayList<>();

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        calculateTotalInputCost();
    }

    /**
     * Crop seasons in Indian agriculture.
     * Requirements: 11A.4
     */
    public enum Season {
        KHARIF,    // Monsoon season (June-October)
        RABI,      // Winter season (October-March)
        ZAID       // Summer season (March-June)
    }

    /**
     * Crop status for tracking growth cycle.
     * Requirements: 11A.4
     */
    public enum CropStatus {
        SOWN,
        GROWING,
        HARVESTED,
        FAILED
    }

    /**
     * Calculate total input cost from individual cost components.
     * Requirements: 11A.4
     */
    @PrePersist
    @PreUpdate
    public void calculateTotalInputCost() {
        BigDecimal total = BigDecimal.ZERO;
        if (seedCost != null) total = total.add(seedCost);
        if (fertilizerCost != null) total = total.add(fertilizerCost);
        if (pesticideCost != null) total = total.add(pesticideCost);
        if (laborCost != null) total = total.add(laborCost);
        if (otherCost != null) total = total.add(otherCost);
        this.totalInputCost = total;
    }

    /**
     * Calculate total revenue from yield and selling price.
     * Requirements: 11A.5
     */
    public void calculateTotalRevenue() {
        if (totalYieldQuintals != null && sellingPricePerQuintal != null) {
            this.totalRevenue = totalYieldQuintals.multiply(sellingPricePerQuintal);
        }
    }

    /**
     * Add a fertilizer application to this crop.
     */
    public void addFertilizerApplication(FertilizerApplication application) {
        fertilizerApplications.add(application);
        application.setCrop(this);
    }

    /**
     * Get profit/loss for this crop.
     */
    public BigDecimal getProfitLoss() {
        if (totalRevenue == null || totalInputCost == null) {
            return null;
        }
        return totalRevenue.subtract(totalInputCost);
    }
}