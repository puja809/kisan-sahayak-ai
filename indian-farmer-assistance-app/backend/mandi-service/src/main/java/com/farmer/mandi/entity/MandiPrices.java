package com.farmer.mandi.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity representing mandi (agricultural market) price data.
 * Stores commodity prices from AGMARKNET for various mandis.
 * 
 * Requirements:
 * - 6.1: Retrieve current prices from AGMARKNET API
 * - 6.2: Display modal price, min price, max price, arrival quantity, variety
 * - 6.11: Display cached prices with timestamp when AGMARKNET unavailable
 */
@Entity
@Table(name = "mandi_prices", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"commodity_name", "variety", "mandi_code", "price_date"}),
       indexes = {
           @Index(name = "idx_commodity", columnList = "commodity_name"),
           @Index(name = "idx_mandi", columnList = "mandi_name"),
           @Index(name = "idx_location", columnList = "state, district"),
           @Index(name = "idx_date", columnList = "price_date")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MandiPrices {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "commodity_name", nullable = false, length = 100)
    private String commodityName;

    @Column(name = "variety", length = 100)
    private String variety;

    @Column(name = "mandi_name", nullable = false, length = 100)
    private String mandiName;

    @Column(name = "mandi_code", length = 50)
    private String mandiCode;

    @Column(name = "state", nullable = false, length = 50)
    private String state;

    @Column(name = "district", nullable = false, length = 50)
    private String district;

    @Column(name = "price_date", nullable = false)
    private LocalDate priceDate;

    @Column(name = "modal_price")
    private Double modalPrice;

    @Column(name = "min_price")
    private Double minPrice;

    @Column(name = "max_price")
    private Double maxPrice;

    @Column(name = "arrival_quantity_quintals")
    private Double arrivalQuantityQuintals;

    @Column(name = "unit", length = 20)
    @Builder.Default
    private String unit = "Quintal";

    @Column(name = "source", length = 50)
    @Builder.Default
    private String source = "AGMARKNET";

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /**
     * Validates that min_price <= modal_price <= max_price.
     * This is a key constraint for price data integrity.
     * 
     * Property 12: Price Data Completeness and Constraints
     * Validates: Requirements 6.2
     */
    public boolean isPriceConstraintValid() {
        if (minPrice == null || modalPrice == null || maxPrice == null) {
            return false;
        }
        return (minPrice <= modalPrice && modalPrice <= maxPrice);
    }
}