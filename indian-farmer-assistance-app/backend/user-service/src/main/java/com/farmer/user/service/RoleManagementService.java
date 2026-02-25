package com.farmer.user.service;

import com.farmer.user.dto.AdminCreationRequest;
import com.farmer.user.dto.RoleModificationRequest;
import com.farmer.user.dto.RoleModificationResponse;
import com.farmer.user.entity.RoleModificationAudit;
import com.farmer.user.entity.User;
import com.farmer.user.repository.RoleModificationAuditRepository;
import com.farmer.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing user roles with audit logging.
 * Requirements: 22.1, 22.2, 22.7
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoleManagementService {

    private final UserRepository userRepository;
    private final RoleModificationAuditRepository auditRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.super-admin-token:super-admin-secret-token}")
    private String superAdminToken;

    private static final String ADMIN_CREATION_REASON = "Admin account created by super admin";
    private static final String ROLE_CHANGE_REASON = "Role modification by admin";

    /**
     * Create a new admin account with super-admin approval.
     * Requirements: 22.2
     */
    @Transactional
    public RoleModificationResponse createAdmin(AdminCreationRequest request, User creator, HttpServletRequest httpRequest) {
        // Validate super admin token
        if (!request.getSuperAdminToken().equals(superAdminToken)) {
            log.warn("Unauthorized admin creation attempt by user: {}", creator.getFarmerId());
            return RoleModificationResponse.builder()
                    .success(false)
                    .message("Invalid super admin token. Admin creation denied.")
                    .build();
        }

        // Check if user already exists with the same phone
        Optional<User> existingUser = userRepository.findByPhone(request.getPhone());
        User user;

        if (existingUser.isPresent()) {
            user = existingUser.get();
            if (user.getRole() == User.Role.ADMIN) {
                return RoleModificationResponse.builder()
                        .success(false)
                        .message("User is already an admin.")
                        .build();
            }
        } else {
            // Create new user with ADMIN role
            String farmerId = "FARMER-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            String aadhaarHash = passwordEncoder.encode("ADMIN-" + request.getPhone());

            user = User.builder()
                    .farmerId(farmerId)
                    .aadhaarHash(aadhaarHash)
                    .name(request.getName())
                    .phone(request.getPhone())
                    .email(request.getEmail())
                    .state(request.getState())
                    .district(request.getDistrict())
                    .role(User.Role.ADMIN)
                    .isActive(true)
                    .build();

            user = userRepository.save(user);
        }

        // Create audit record
        RoleModificationAudit audit = createAuditRecord(
                user,
                User.Role.FARMER,
                User.Role.ADMIN,
                creator,
                request.getReason() != null ? request.getReason() : ADMIN_CREATION_REASON,
                getClientIp(httpRequest),
                getClientUserAgent(httpRequest)
        );

        log.info("Admin account created: {} by super admin: {}", user.getFarmerId(), creator.getFarmerId());

        return RoleModificationResponse.builder()
                .auditId(audit.getId())
                .farmerId(user.getFarmerId())
                .oldRole(User.Role.FARMER)
                .newRole(User.Role.ADMIN)
                .modifierId(creator.getFarmerId())
                .modifierName(creator.getName())
                .reason(request.getReason() != null ? request.getReason() : ADMIN_CREATION_REASON)
                .modifiedAt(audit.getCreatedAt())
                .success(true)
                .message("Admin account created successfully.")
                .build();
    }

    /**
     * Modify user role with audit logging.
     * Requirements: 22.2, 22.7
     */
    @Transactional
    public RoleModificationResponse modifyRole(RoleModificationRequest request, User modifier, HttpServletRequest httpRequest) {
        // Find the user to modify
        Optional<User> userOptional = userRepository.findByFarmerId(request.getFarmerId());
        if (userOptional.isEmpty()) {
            return RoleModificationResponse.builder()
                    .success(false)
                    .message("User not found: " + request.getFarmerId())
                    .build();
        }

        User userToModify = userOptional.get();
        User.Role oldRole = userToModify.getRole();
        User.Role newRole = request.getNewRole();

        // Validate role change
        if (oldRole == newRole) {
            return RoleModificationResponse.builder()
                    .success(false)
                    .message("User already has the requested role.")
                    .build();
        }

        // Prevent demoting the last admin
        if (oldRole == User.Role.ADMIN && newRole == User.Role.FARMER) {
            long adminCount = userRepository.countByRole(User.Role.ADMIN);
            if (adminCount <= 1) {
                return RoleModificationResponse.builder()
                        .success(false)
                        .message("Cannot demote the last admin user.")
                        .build();
            }
        }

        // Update user role
        userToModify.setRole(newRole);
        userRepository.save(userToModify);

        // Create audit record
        RoleModificationAudit audit = createAuditRecord(
                userToModify,
                oldRole,
                newRole,
                modifier,
                request.getReason() != null ? request.getReason() : ROLE_CHANGE_REASON,
                getClientIp(httpRequest),
                getClientUserAgent(httpRequest)
        );

        log.info("Role modified: {} from {} to {} by admin: {}",
                userToModify.getFarmerId(), oldRole, newRole, modifier.getFarmerId());

        return RoleModificationResponse.builder()
                .auditId(audit.getId())
                .farmerId(userToModify.getFarmerId())
                .oldRole(oldRole)
                .newRole(newRole)
                .modifierId(modifier.getFarmerId())
                .modifierName(modifier.getName())
                .reason(request.getReason() != null ? request.getReason() : ROLE_CHANGE_REASON)
                .modifiedAt(audit.getCreatedAt())
                .success(true)
                .message("Role modified successfully.")
                .build();
    }

    /**
     * Get role modification history for a user.
     * Requirements: 22.7
     */
    public List<RoleModificationAudit> getRoleHistory(String farmerId) {
        return auditRepository.findByFarmerIdOrderByCreatedAtDesc(farmerId);
    }

    /**
     * Get all role modifications within a date range.
     * Requirements: 22.7
     */
    public List<RoleModificationAudit> getModificationsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return auditRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(startDate, endDate);
    }

    /**
     * Get all admin users.
     * Requirements: 22.1
     */
    public List<User> getAllAdmins() {
        return userRepository.findByRoleAndIsActiveTrue(User.Role.ADMIN);
    }

    /**
     * Get all farmers.
     * Requirements: 22.1
     */
    public List<User> getAllFarmers() {
        return userRepository.findByRoleAndIsActiveTrue(User.Role.FARMER);
    }

    /**
     * Create an audit record for role modification.
     * Requirements: 22.2, 22.7
     */
    private RoleModificationAudit createAuditRecord(
            User user,
            User.Role oldRole,
            User.Role newRole,
            User modifier,
            String reason,
            String ipAddress,
            String userAgent) {

        RoleModificationAudit audit = RoleModificationAudit.builder()
                .userId(user.getId())
                .farmerId(user.getFarmerId())
                .oldRole(oldRole)
                .newRole(newRole)
                .modifierId(modifier.getFarmerId())
                .modifierName(modifier.getName())
                .reason(reason)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();

        return auditRepository.save(audit);
    }

    /**
     * Get client IP address from request.
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Get client user agent from request.
     */
    private String getClientUserAgent(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent != null && userAgent.length() > 500) {
            return userAgent.substring(0, 500);
        }
        return userAgent;
    }
}