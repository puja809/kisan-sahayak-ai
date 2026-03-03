package com.farmer.scheme.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "schemes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Scheme {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 1000)
    private String schemeName;
    
    @Column(length = 500)
    private String centerStateName;
    
    @Column(length = 4000)
    private String schemeDetails;
    
    @Column(length = 1000)
    private String websiteLink;
    
    @Column(length = 100)
    private String phone;
    
    @Column(length = 200)
    private String email;
    
    @Column(length = 500)
    private String commodityName;
    
    @Column(length = 500)
    private String responsibleMinistry;
    
    @Column(length = 1000)
    private String implementingOffice;
    
    @Column(length = 4000)
    private String officeAddress;
    
    @Column(length = 4000)
    private String addressSource;
}
