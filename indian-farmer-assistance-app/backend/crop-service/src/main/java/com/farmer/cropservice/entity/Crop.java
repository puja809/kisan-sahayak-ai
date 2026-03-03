package com.farmer.cropservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "crops")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Crop {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String commodity;

    private String category;
    private String season;
    private String durationDays;
    private String seedRateKgPerAcre;
    private String spacingCm;
    private String fertilizerNpkKgPerAcre;
    private String irrigationNumber;
    private String keyOperations;
    private String harvestSigns;
    private String yieldKgPerAcre;
}
