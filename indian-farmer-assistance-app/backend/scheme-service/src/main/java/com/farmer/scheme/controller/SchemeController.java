package com.farmer.scheme.controller;

import com.farmer.scheme.dto.SchemeDTO;
import com.farmer.scheme.service.SchemeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/schemes")
@RequiredArgsConstructor
public class SchemeController {
    private final SchemeService schemeService;

    @GetMapping
    public ResponseEntity<Page<SchemeDTO>> searchSchemes(
            @RequestParam(required = false) String commodity,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String center,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<SchemeDTO> schemes = schemeService.searchSchemes(commodity, state, center, pageable);
        return ResponseEntity.ok(schemes);
    }
}
