package com.farmer.mandi.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "variety",
       uniqueConstraints = @UniqueConstraint(columnNames = {"commodity_id", "variety_name"}),
       indexes = {
           @Index(name = "idx_commodity_id", columnList = "commodity_id"),
           @Index(name = "idx_variety_name", columnList = "variety_name")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Variety {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "variety_name", nullable = false, length = 100)
    private String varietyName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commodity_id", nullable = false)
    private Commodity commodity;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at")
    private java.time.LocalDateTime createdAt;

    @Column(name = "updated_at")
    private java.time.LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = java.time.LocalDateTime.now();
        updatedAt = java.time.LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = java.time.LocalDateTime.now();
    }
}
