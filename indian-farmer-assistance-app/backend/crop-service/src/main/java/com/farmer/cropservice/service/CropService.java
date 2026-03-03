package com.farmer.cropservice.service;

import com.farmer.cropservice.dto.CropDTO;
import com.farmer.cropservice.entity.Crop;
import com.farmer.cropservice.repository.CropRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CropService {
    private final CropRepository cropRepository;

    @Transactional
    public void loadCropsFromCsv() {
        try {
            if (cropRepository.count() > 0) {
                log.info("Crops already loaded in database");
                return;
            }

            ClassPathResource resource = new ClassPathResource("data/Complete_Crop_Cultivation_Database_80_Crops.csv");
            BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()));

            String line;
            boolean isHeader = true;
            while ((line = reader.readLine()) != null) {
                if (isHeader) {
                    isHeader = false;
                    continue;
                }

                String[] fields = parseCsvLine(line);
                if (fields.length >= 11) {
                    Crop crop = new Crop();
                    crop.setCommodity(fields[0].trim());
                    crop.setCategory(fields[1].trim());
                    crop.setSeason(fields[2].trim());
                    crop.setDurationDays(fields[3].trim());
                    crop.setSeedRateKgPerAcre(fields[4].trim());
                    crop.setSpacingCm(fields[5].trim());
                    crop.setFertilizerNpkKgPerAcre(fields[6].trim());
                    crop.setIrrigationNumber(fields[7].trim());
                    crop.setKeyOperations(fields[8].trim());
                    crop.setHarvestSigns(fields[9].trim());
                    crop.setYieldKgPerAcre(fields[10].trim());

                    cropRepository.save(crop);
                }
            }
            reader.close();
            log.info("Successfully loaded crops from CSV");
        } catch (Exception e) {
            log.error("Error loading crops from CSV", e);
        }
    }

    private String[] parseCsvLine(String line) {
        List<String> fields = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());
        return fields.toArray(new String[0]);
    }

    public List<CropDTO> getAllCrops() {
        return cropRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public CropDTO getCropByCommodity(String commodity) {
        return cropRepository.findByCommodity(commodity)
                .map(this::convertToDTO)
                .orElse(null);
    }

    private CropDTO convertToDTO(Crop crop) {
        return new CropDTO(
                crop.getId(),
                crop.getCommodity(),
                crop.getCategory(),
                crop.getSeason(),
                crop.getDurationDays(),
                crop.getSeedRateKgPerAcre(),
                crop.getSpacingCm(),
                crop.getFertilizerNpkKgPerAcre(),
                crop.getIrrigationNumber(),
                crop.getKeyOperations(),
                crop.getHarvestSigns(),
                crop.getYieldKgPerAcre()
        );
    }
}
