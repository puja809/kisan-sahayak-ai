package com.farmer.mandi.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity representing commodity/crop types.
 * Stores commodity information for market data.
 * 
 * Requirements:
 * - 6.1: Retrieve current prices from AGMARKNET API
 * - 6.2: Display modal price, min price, max price, arrival quantity, variety
 */
@Entity
@Table(name = "commodity",
       indexes = {
           @Index(name = "idx_commodity_name", columnList = "commodity_name")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Commodity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "commodity_name", nullable = false, unique = true, length = 100)
    private String commodityName;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at")
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