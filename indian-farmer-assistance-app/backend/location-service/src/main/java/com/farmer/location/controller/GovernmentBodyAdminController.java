package com.farmer.location.controller;

import com.farmer.location.dto.GovernmentBodyCreateRequest;
import com.farmer.location.dto.GovernmentBodyDto;
import com.farmer.location.entity.GovernmentBody;
import com.farmer.location.service.GovernmentBodyService;
import com.farmer.location.util.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/government-bodies/admin")
@RequiredArgsConstructor
@Slf4j
public class GovernmentBodyAdminController {
    
    private final GovernmentBodyService governmentBodyService;
    private final JwtUtil jwtUtil;
    
    @PostMapping("/create")
    public ResponseEntity<GovernmentBodyDto> createGovernmentBody(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody GovernmentBodyCreateRequest request) {
        
        String token = extractToken(authHeader);
        if (token == null || !jwtUtil.isTokenValid(token) || !jwtUtil.isAdmin(token)) {
            log.warn("Unauthorized create attempt");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        log.info("Admin creating government body for state: {}, district: {}", request.getState(), request.getDistrict());
        
        GovernmentBody body = GovernmentBody.builder()
                .state(request.getState())
                .district(request.getDistrict())
                .districtOfficer(request.getDistrictOfficer())
                .districtPhone(request.getDistrictPhone())
                .email(request.getEmail())
                .kvkPhone(request.getKvkPhone())
                .sampleVillage(request.getSampleVillage())
                .build();
        
        GovernmentBody saved = governmentBodyService.createGovernmentBody(body);
        GovernmentBodyDto dto = mapToDto(saved);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<GovernmentBodyDto> updateGovernmentBody(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id,
            @Valid @RequestBody GovernmentBodyCreateRequest request) {
        
        String token = extractToken(authHeader);
        if (token == null || !jwtUtil.isTokenValid(token) || !jwtUtil.isAdmin(token)) {
            log.warn("Unauthorized update attempt for id: {}", id);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        log.info("Admin updating government body id: {}", id);
        
        GovernmentBody body = GovernmentBody.builder()
                .id(id)
                .state(request.getState())
                .district(request.getDistrict())
                .districtOfficer(request.getDistrictOfficer())
                .districtPhone(request.getDistrictPhone())
                .email(request.getEmail())
                .kvkPhone(request.getKvkPhone())
                .sampleVillage(request.getSampleVillage())
                .build();
        
        GovernmentBody updated = governmentBodyService.updateGovernmentBody(id, body);
        if (updated == null) {
            log.warn("Government body not found for id: {}", id);
            return ResponseEntity.notFound().build();
        }
        
        GovernmentBodyDto dto = mapToDto(updated);
        return ResponseEntity.ok(dto);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGovernmentBody(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
        
        String token = extractToken(authHeader);
        if (token == null || !jwtUtil.isTokenValid(token) || !jwtUtil.isAdmin(token)) {
            log.warn("Unauthorized delete attempt for id: {}", id);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        log.info("Admin deleting government body id: {}", id);
        
        boolean deleted = governmentBodyService.deleteGovernmentBody(id);
        if (!deleted) {
            log.warn("Government body not found for deletion, id: {}", id);
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.noContent().build();
    }
    
    private String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
    
    private GovernmentBodyDto mapToDto(GovernmentBody body) {
        return GovernmentBodyDto.builder()
                .id(body.getId())
                .state(body.getState())
                .district(body.getDistrict())
                .districtOfficer(body.getDistrictOfficer())
                .districtPhone(body.getDistrictPhone())
                .email(body.getEmail())
                .kvkPhone(body.getKvkPhone())
                .sampleVillage(body.getSampleVillage())
                .build();
    }
}
