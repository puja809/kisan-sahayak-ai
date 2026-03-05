package com.farmer.user.service;

import com.farmer.user.dto.*;
import com.farmer.user.entity.Crop;
import com.farmer.user.entity.User;
import com.farmer.user.repository.CropRepository;
import com.farmer.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for farmer profile and crop management.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileService {

        private final UserRepository userRepository;
        private final CropRepository cropRepository;

        @Transactional
        public CropResponse addCrop(String farmerId, CropRequest request) {
                log.info("Adding crop for farmer: {}", farmerId);

                User user = userRepository.findByFarmerId(farmerId)
                                .orElseThrow(() -> new UserService.UserNotFoundException(
                                                "User not found: " + farmerId));

                Crop crop = Crop.builder()
                                .user(user)
                                .cropName(request.getCropName())
                                .cropVariety(request.getCropVariety())
                                .sowingDate(request.getSowingDate())
                                .expectedHarvestDate(request.getExpectedHarvestDate())
                                .areaAcres(request.getAreaAcres())
                                .season(request.getSeason())
                                .status(request.getStatus() != null ? request.getStatus() : Crop.CropStatus.SOWN)
                                .seedCost(request.getSeedCost())
                                .fertilizerCost(request.getFertilizerCost())
                                .pesticideCost(request.getPesticideCost())
                                .laborCost(request.getLaborCost())
                                .otherCost(request.getOtherCost())
                                .totalYieldQuintals(request.getTotalYieldQuintals())
                                .qualityGrade(request.getQualityGrade())
                                .sellingPricePerQuintal(request.getSellingPricePerQuintal())
                                .mandiName(request.getMandiName())
                                .totalRevenue(request.getTotalRevenue())
                                .actualHarvestDate(request.getActualHarvestDate())
                                .notes(request.getNotes())
                                .build();

                crop = cropRepository.save(crop);
                log.info("Crop added successfully: {}", crop.getId());
                return CropResponse.fromEntity(crop);
        }

        public List<CropResponse> getUserCrops(String farmerId) {
                User user = userRepository.findByFarmerId(farmerId)
                                .orElseThrow(() -> new UserService.UserNotFoundException(
                                                "User not found: " + farmerId));

                return cropRepository.findByUserId(user.getId()).stream()
                                .map(CropResponse::fromEntity)
                                .collect(Collectors.toList());
        }

        public List<CropResponse> getCurrentCrops(String farmerId) {
                User user = userRepository.findByFarmerId(farmerId)
                                .orElseThrow(() -> new UserService.UserNotFoundException(
                                                "User not found: " + farmerId));

                return cropRepository.findCurrentCropsByUserId(user.getId()).stream()
                                .map(CropResponse::fromEntity)
                                .collect(Collectors.toList());
        }

        public CropResponse getCrop(String farmerId, Long cropId) {
                User user = userRepository.findByFarmerId(farmerId)
                                .orElseThrow(() -> new UserService.UserNotFoundException(
                                                "User not found: " + farmerId));

                Crop crop = cropRepository.findByIdAndUserId(cropId, user.getId())
                                .orElseThrow(() -> new RuntimeException("Crop not found: " + cropId));

                return CropResponse.fromEntity(crop);
        }

        @Transactional
        public CropResponse updateCrop(String farmerId, Long cropId, CropRequest request) {
                User user = userRepository.findByFarmerId(farmerId)
                                .orElseThrow(() -> new UserService.UserNotFoundException(
                                                "User not found: " + farmerId));

                Crop crop = cropRepository.findByIdAndUserId(cropId, user.getId())
                                .orElseThrow(() -> new RuntimeException("Crop not found: " + cropId));

                if (request.getCropName() != null)
                        crop.setCropName(request.getCropName());
                if (request.getCropVariety() != null)
                        crop.setCropVariety(request.getCropVariety());
                if (request.getExpectedHarvestDate() != null)
                        crop.setExpectedHarvestDate(request.getExpectedHarvestDate());
                if (request.getAreaAcres() != null)
                        crop.setAreaAcres(request.getAreaAcres());
                if (request.getSeason() != null)
                        crop.setSeason(request.getSeason());
                if (request.getStatus() != null)
                        crop.setStatus(request.getStatus());
                if (request.getSeedCost() != null)
                        crop.setSeedCost(request.getSeedCost());
                if (request.getFertilizerCost() != null)
                        crop.setFertilizerCost(request.getFertilizerCost());
                if (request.getPesticideCost() != null)
                        crop.setPesticideCost(request.getPesticideCost());
                if (request.getLaborCost() != null)
                        crop.setLaborCost(request.getLaborCost());
                if (request.getOtherCost() != null)
                        crop.setOtherCost(request.getOtherCost());
                if (request.getTotalYieldQuintals() != null)
                        crop.setTotalYieldQuintals(request.getTotalYieldQuintals());
                if (request.getQualityGrade() != null)
                        crop.setQualityGrade(request.getQualityGrade());
                if (request.getSellingPricePerQuintal() != null)
                        crop.setSellingPricePerQuintal(request.getSellingPricePerQuintal());
                if (request.getMandiName() != null)
                        crop.setMandiName(request.getMandiName());
                if (request.getTotalRevenue() != null)
                        crop.setTotalRevenue(request.getTotalRevenue());
                if (request.getActualHarvestDate() != null)
                        crop.setActualHarvestDate(request.getActualHarvestDate());
                if (request.getNotes() != null)
                        crop.setNotes(request.getNotes());

                crop = cropRepository.save(crop);
                return CropResponse.fromEntity(crop);
        }

        @Transactional
        public CropResponse recordHarvest(String farmerId, HarvestRequest request) {
                User user = userRepository.findByFarmerId(farmerId)
                                .orElseThrow(() -> new UserService.UserNotFoundException(
                                                "User not found: " + farmerId));

                Crop crop = cropRepository.findByIdAndUserId(request.getCropId(), user.getId())
                                .orElseThrow(() -> new RuntimeException("Crop not found: " + request.getCropId()));

                if (request.getActualHarvestDate() != null)
                        crop.setActualHarvestDate(request.getActualHarvestDate());
                if (request.getTotalYieldQuintals() != null)
                        crop.setTotalYieldQuintals(request.getTotalYieldQuintals());
                if (request.getQualityGrade() != null)
                        crop.setQualityGrade(request.getQualityGrade());
                if (request.getSellingPricePerQuintal() != null)
                        crop.setSellingPricePerQuintal(request.getSellingPricePerQuintal());
                if (request.getMandiName() != null)
                        crop.setMandiName(request.getMandiName());

                crop.setStatus(Crop.CropStatus.HARVESTED);
                crop = cropRepository.save(crop);
                return CropResponse.fromEntity(crop);
        }

        @Transactional
        public void deleteCrop(String farmerId, Long cropId) {
                User user = userRepository.findByFarmerId(farmerId)
                                .orElseThrow(() -> new UserService.UserNotFoundException(
                                                "User not found: " + farmerId));

                Crop crop = cropRepository.findByIdAndUserId(cropId, user.getId())
                                .orElseThrow(() -> new RuntimeException("Crop not found: " + cropId));

                cropRepository.delete(crop);
        }
}