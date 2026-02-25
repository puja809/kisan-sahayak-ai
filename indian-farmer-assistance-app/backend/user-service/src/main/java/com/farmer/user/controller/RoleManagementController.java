package com.farmer.user.controller;

import com.farmer.user.dto.AdminCreationRequest;
import com.farmer.user.dto.RoleModificationRequest;
import com.farmer.user.dto.RoleModificationResponse;
import com.farmer.user.entity.RoleModificationAudit;
import com.farmer.user.entity.User;
import com.farmer.user.service.RoleManagementService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Controller for role management operations.
 * Requirements: 22.1, 22.2, 22.7
 */
@RestController
@RequestMapping("/api/v1/admin/roles")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class RoleManagementController {

    private final RoleManagementService roleManagementService;

    /**
     * Create a new admin account.
     * Requires super admin token for approval.
     * Requirements: 22.2
     */
    @PostMapping("/create-admin")
    public ResponseEntity<RoleModificationResponse> createAdmin(
            @Valid @RequestBody AdminCreationRequest request,
            @AuthenticationPrincipal User currentUser,
            HttpServletRequest httpRequest) {

        log.info("Admin creation requested by: {} for phone: {}",
                currentUser.getFarmerId(), request.getPhone());

        RoleModificationResponse response = roleManagementService.createAdmin(request, currentUser, httpRequest);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Modify user role.
     * Requirements: 22.2, 22.7
     */
    @PostMapping("/modify")
    public ResponseEntity<RoleModificationResponse> modifyRole(
            @Valid @RequestBody RoleModificationRequest request,
            @AuthenticationPrincipal User currentUser,
            HttpServletRequest httpRequest) {

        log.info("Role modification requested by: {} for user: {}",
                currentUser.getFarmerId(), request.getFarmerId());

        RoleModificationResponse response = roleManagementService.modifyRole(request, currentUser, httpRequest);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get role modification history for a user.
     * Requirements: 22.7
     */
    @GetMapping("/history/{farmerId}")
    public ResponseEntity<List<RoleModificationAudit>> getRoleHistory(@PathVariable String farmerId) {
        List<RoleModificationAudit> history = roleManagementService.getRoleHistory(farmerId);
        return ResponseEntity.ok(history);
    }

    /**
     * Get all role modifications within a date range.
     * Requirements: 22.7
     */
    @GetMapping("/audit")
    public ResponseEntity<List<RoleModificationAudit>> getAuditLogs(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        List<RoleModificationAudit> logs = roleManagementService.getModificationsByDateRange(startDate, endDate);
        return ResponseEntity.ok(logs);
    }

    /**
     * Get all admin users.
     * Requirements: 22.1
     */
    @GetMapping("/admins")
    public ResponseEntity<List<User>> getAllAdmins() {
        List<User> admins = roleManagementService.getAllAdmins();
        return ResponseEntity.ok(admins);
    }

    /**
     * Get all farmers.
     * Requirements: 22.1
     */
    @GetMapping("/farmers")
    public ResponseEntity<List<User>> getAllFarmers() {
        List<User> farmers = roleManagementService.getAllFarmers();
        return ResponseEntity.ok(farmers);
    }
}