package com.farmer.user.controller;

import com.farmer.user.dto.AdminCreationRequest;
import com.farmer.user.dto.RoleModificationRequest;
import com.farmer.user.dto.RoleModificationResponse;
import com.farmer.user.entity.User;
import com.farmer.user.service.RoleManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for role management operations.
 */
@RestController
@RequestMapping("/api/v1/admin/roles")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class RoleManagementController {

    private final RoleManagementService roleManagementService;

    @PostMapping("/create-admin")
    public ResponseEntity<RoleModificationResponse> createAdmin(
            @Valid @RequestBody AdminCreationRequest request,
            @AuthenticationPrincipal User currentUser) {
        log.info("Admin creation requested by: {} for email: {}", currentUser.getFarmerId(), request.getEmail());
        RoleModificationResponse response = roleManagementService.createAdmin(request, currentUser);
        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }

    @PostMapping("/modify")
    public ResponseEntity<RoleModificationResponse> modifyRole(
            @Valid @RequestBody RoleModificationRequest request) {
        RoleModificationResponse response = roleManagementService.modifyRole(request);
        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }

    @GetMapping("/admins")
    public ResponseEntity<List<User>> getAllAdmins() {
        return ResponseEntity.ok(roleManagementService.getAllAdmins());
    }

    @GetMapping("/farmers")
    public ResponseEntity<List<User>> getAllFarmers() {
        return ResponseEntity.ok(roleManagementService.getAllFarmers());
    }
}