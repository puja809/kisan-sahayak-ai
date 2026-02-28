package com.farmer.mandi.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity representing commodity grades.
 * Stores grade information for commodities and varieties.
 */
@Entity
@Table(name = "grade",
       uniqueConstraints = @UniqueConstraint(columnNames = {"variety_id", "grade_name"}),
       indexes = {
           @Index(name = "idx_variety_id", columnList = "variety_id"),
           @Index(name = "idx_grade_name", columnList = "grade_name")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Grade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variety_id", nullable = false)
    private Variety variety;

    @Column(name = "grade_name", nullable = false, length = 50)
    private String gradeName;

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
