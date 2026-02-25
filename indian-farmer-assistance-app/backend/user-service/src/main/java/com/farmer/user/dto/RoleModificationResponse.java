package com.farmer.user.dto;

import com.farmer.user.entity.User;
import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO for role modification response.
 * Requirements: 22.2, 22.7
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleModificationResponse {

    private Long auditId;
    private String farmerId;
    private User.Role oldRole;
    private User.Role newRole;
    private String modifierId;
    private String modifierName;
    private String reason;
    private LocalDateTime modifiedAt;
    private String message;
    private boolean success;
}