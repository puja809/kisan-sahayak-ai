package com.farmer.mandi.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Entity representing price alerts subscribed by farmers.
 * Stores alert configurations for crop price notifications.
 * 
 * Requirements:
 * - 6.10: Send push notifications for crop price alerts
 */
@Entity
@Table(name = "price_alerts", 
       indexes = {
           @Index(name = "idx_farmer_id", columnList = "farmer_id"),
           @Index(name = "idx_commodity", columnList = "commodity"),
           @Index(name = "idx_active", columnList = "is_active")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "farmer_id", nullable = false, length = 50)
    private String farmerId;

    @Column(name = "commodity", nullable = false, length = 100)
    private String commodity;

    @Column(name = "variety", length = 100)
    private String variety;

    @Column(name = "target_price")
    private Double targetPrice;

    @Column(name = "alert_type", length = 20)
    @Builder.Default
    private String alertType = "PRICE_ABOVE"; // PRICE_ABOVE, PRICE_BELOW, PRICE_PEAK

    @Column(name = "neighboring_districts_only")
    @Builder.Default
    private Boolean neighboringDistrictsOnly = false;

    @Column(name = "notification_sent")
    @Builder.Default
    private Boolean notificationSent = false;

    @Column(name = "last_notification_at")
    private LocalDateTime lastNotificationAt;

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