package com.farmer.location.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "government_bodies", indexes = {
    @Index(name = "idx_state", columnList = "state"),
    @Index(name = "idx_district", columnList = "district"),
    @Index(name = "idx_state_district", columnList = "state,district")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GovernmentBody {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String state;
    
    @Column(nullable = false)
    private String district;
    
    @Column(name = "district_officer")
    private String districtOfficer;
    
    @Column(name = "district_phone")
    private String districtPhone;
    
    @Column(name = "email")
    private String email;
    
    @Column(name = "kvk_phone")
    private String kvkPhone;
    
    @Column(name = "sample_village")
    private String sampleVillage;
    
    @Column(name = "body_type", nullable = true)
    private String bodyType;
}
