package com.farmer.user.service;

import com.farmer.user.dto.AdminCreationRequest;
import com.farmer.user.dto.RoleModificationRequest;
import com.farmer.user.dto.RoleModificationResponse;
import com.farmer.user.entity.User;
import com.farmer.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing user roles.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoleManagementService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.super-admin-token:super-admin-secret-token}")
    private String superAdminToken;

    @Transactional
    public RoleModificationResponse createAdmin(AdminCreationRequest request, User creator) {
        if (!request.getSuperAdminToken().equals(superAdminToken)) {
            return RoleModificationResponse.builder().success(false).message("Invalid token").build();
        }

        User user = userRepository.findByEmail(request.getEmail()).orElse(null);
        if (user != null) {
            user.setRole(User.Role.ADMIN);
            if (request.getPassword() != null) {
                user.setPassword(passwordEncoder.encode(request.getPassword()));
            }
        } else {
            user = User.builder()
                    .farmerId("FARMER-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                    .name(request.getName())
                    .email(request.getEmail())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .role(User.Role.ADMIN)
                    .isActive(true)
                    .build();
        }
        userRepository.save(user);
        return RoleModificationResponse.builder().success(true).message("Admin created").build();
    }

    @Transactional
    public RoleModificationResponse modifyRole(RoleModificationRequest request) {
        User user = userRepository.findByFarmerId(request.getFarmerId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setRole(request.getNewRole());
        userRepository.save(user);
        return RoleModificationResponse.builder().success(true).message("Role updated").build();
    }

    public List<User> getAllAdmins() {
        return userRepository.findByRole(User.Role.ADMIN);
    }

    public List<User> getAllFarmers() {
        return userRepository.findByRole(User.Role.FARMER);
    }
}