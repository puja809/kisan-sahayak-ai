package com.farmer.mandi.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for price alert subscription request/response.
 * 
 * Requirements:
 * - 6.10: Create price alert subscription endpoints
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceAlertDto {

    private Long id;
    private String farmerId;
    private String commodity;
    private String variety;
    private BigDecimal targetPrice;
    private String alertType;
    private Boolean neighboringDistrictsOnly;
    private Boolean isActive;
    private Boolean notificationSent;
    private LocalDateTime lastNotificationAt;
    private LocalDateTime createdAt;
}