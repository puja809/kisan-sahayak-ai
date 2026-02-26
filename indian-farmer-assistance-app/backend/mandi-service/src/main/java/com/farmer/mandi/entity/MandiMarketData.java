package com.farmer.mandi.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Entity for Mandi Market Data
 * Stores market information for filtering and search
 */
@Entity
@Table(name = "mandi_market_data", indexes = {
    @Index(name = "idx_state", columnList = "state"),
    @Index(name = "idx_district", columnList = "district"),
    @Index(name = "idx_market", columnList = "market"),
    @Index(name = "idx_commodity", columnList = "commodity"),
    @Index(name = "idx_variety", columnList = "variety"),
    @Index(name = "idx_grade", columnList = "grade"),
    @Index(name = "idx_state_district", columnList = "state,district"),
    @Index(name = "idx_market_commodity", columnList = "market,commodity"),
    @Index(name = "idx_state_id", columnList = "state_id"),
    @Index(name = "idx_district_id", columnList = "district_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MandiMarketData {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 100)
    private String state;
    
    @Column(nullable = false, length = 100)
    private String district;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "state_id")
    private State stateEntity;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "district_id")
    private District districtEntity;
    
    @Column(nullable = false, length = 200)
    private String market;
    
    @Column(nullable = false, length = 100)
    private String commodity;
    
    @Column(nullable = false, length = 100)
    private String variety;
    
    @Column(nullable = false, length = 100)
    private String grade;
    
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
