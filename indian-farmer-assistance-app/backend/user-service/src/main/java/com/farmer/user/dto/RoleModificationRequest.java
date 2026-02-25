package com.farmer.user.dto;

import com.farmer.user.entity.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * DTO for role modification requests.
 * Requirements: 22.2, 22.7
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleModificationRequest {

    @NotBlank(message = "Farmer ID is required")
    private String farmerId;

    @NotNull(message = "New role is required")
    private User.Role newRole;

    private String reason;

    private String superAdminToken;
}