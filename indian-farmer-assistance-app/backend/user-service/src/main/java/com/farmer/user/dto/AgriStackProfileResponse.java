package com.farmer.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for AgriStack profile data from the three core registries.
 * Requirements: 11.2, 11.3
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgriStackProfileResponse {

    /**
     * Farmer Registry information.
     */
    private FarmerRegistryInfo farmerRegistry;

    /**
     * Geo-Referenced Village Map Registry information.
     */
    private GeoMapRegistryInfo geoMapRegistry;

    /**
     * Crop Sown Registry information.
     */
    private CropSownRegistryInfo cropSownRegistry;

    /**
     * Whether the profile was successfully retrieved.
     */
    private Boolean success;

    /**
     * Error message if retrieval failed.
     */
    private String errorMessage;

    /**
     * Timestamp of the data retrieval.
     */
    private String retrievedAt;

    /**
     * Farmer Registry information from AgriStack.
     * Requirements: 11.2
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FarmerRegistryInfo {
        private String farmerId;
        private String aadhaarNumber;
        private String name;
        private String gender;
        private String dateOfBirth;
        private String category; // SC, ST, OBC, General
        private String phoneNumber;
        private String state;
        private String district;
        private String block;
        private String gramPanchayat;
        private String village;
        private String pinCode;
        private Double latitude;
        private Double longitude;
    }

    /**
     * Geo-Referenced Village Map Registry information.
     * Requirements: 11.3
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeoMapRegistryInfo {
        private String farmerId;
        private List<LandParcelInfo> landParcels;
        private Double totalLandAreaHectares;
        private String surveyNumber;
        private String tenureType;
        private String landClassification;
    }

    /**
     * Land parcel information from Geo-Referenced Village Map Registry.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LandParcelInfo {
        private String parcelId;
        private String surveyNumber;
        private Double areaHectares;
        private String landType; // Cultivable, Non-cultivable
        private String irrigationSource;
        private String soilType;
        private String geoCoordinates; // WKT format
    }

    /**
     * Crop Sown Registry information from Digital Crop Survey.
     * Requirements: 11.3
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CropSownRegistryInfo {
        private String farmerId;
        private String surveyYear;
        private String season; // Kharif, Rabi, Zaid
        private List<CropInfo> crops;
        private String verificationStatus;
        private String verifiedBy;
        private String verifiedAt;
    }

    /**
     * Crop information from Crop Sown Registry.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CropInfo {
        private String cropName;
        private String variety;
        private Double areaHectares;
        private String sowingDate;
        private String expectedHarvestDate;
        private String irrigationType;
        private String cropCategory;
    }
}