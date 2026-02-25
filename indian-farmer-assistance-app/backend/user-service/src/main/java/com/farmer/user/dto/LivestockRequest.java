package com.farmer.user.dto;

import com.farmer.user.entity.Livestock;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.time.LocalDate;

/**
 * DTO for creating or updating livestock.
 * Requirements: 11A.11
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LivestockRequest {

    private Long farmId;

    @NotNull(message = "Livestock type is required")
    private Livestock.LivestockType livestockType;

    private String breed;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    private Integer quantity;

    private Livestock.LivestockPurpose purpose;

    private LocalDate acquisitionDate;

    private LocalDate dateOfBirth;

    private String gender;

    private String tagNumber;

    private String healthStatus;

    private String vaccinationStatus;

    private String notes;
}