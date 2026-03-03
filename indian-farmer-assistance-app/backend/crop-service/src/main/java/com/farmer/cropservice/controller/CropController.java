package com.farmer.cropservice.controller;

import com.farmer.cropservice.dto.CropDTO;
import com.farmer.cropservice.service.CropService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/crops")
@RequiredArgsConstructor
public class CropController {
    private final CropService cropService;

    @GetMapping
    public ResponseEntity<List<CropDTO>> getAllCrops() {
        return ResponseEntity.ok(cropService.getAllCrops());
    }

    @GetMapping("/commodity/{commodity}")
    public ResponseEntity<CropDTO> getCropByCommodity(@PathVariable String commodity) {
        CropDTO crop = cropService.getCropByCommodity(commodity);
        if (crop != null) {
            return ResponseEntity.ok(crop);
        }
        return ResponseEntity.notFound().build();
    }
}
