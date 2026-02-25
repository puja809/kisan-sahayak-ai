package com.farmer.user.dto;

import com.farmer.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for user profile response.
 * Requirements: 11A.1, 11A.2, 11A.3
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private Long id;
    private String farmerId;
    private String name;
    private String phone;
    private String email;
    private String preferredLanguage;
    private String state;
    private String district;
    private String village;
    private String pinCode;
    private Double gpsLatitude;
    private Double gpsLongitude;
    private String role;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;
    private String agristackFarmerId;
    private Double totalLandholdingAcres;
    private String soilType;
    private String irrigationType;
    private String agroEcologicalZone;

    /**
     * Convert User entity to UserResponse DTO.
     */
    public static UserResponse fromEntity(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .farmerId(user.getFarmerId())
                .name(user.getName())
                .phone(user.getPhone())
                .email(user.getEmail())
                .preferredLanguage(user.getPreferredLanguage())
                .state(user.getState())
                .district(user.getDistrict())
                .village(user.getVillage())
                .pinCode(user.getPinCode())
                .gpsLatitude(user.getGpsLatitude())
                .gpsLongitude(user.getGpsLongitude())
                .role(user.getRole() != null ? user.getRole().name() : null)
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .lastLogin(user.getLastLogin())
                .agristackFarmerId(user.getAgristackFarmerId())
                .totalLandholdingAcres(user.getTotalLandholdingAcres())
                .soilType(user.getSoilType())
                .irrigationType(user.getIrrigationType())
                .agroEcologicalZone(user.getAgroEcologicalZone())
                .build();
    }
}