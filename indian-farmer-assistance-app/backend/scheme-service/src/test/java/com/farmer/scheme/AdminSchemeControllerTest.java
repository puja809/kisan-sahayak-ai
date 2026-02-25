package com.farmer.scheme;

import com.farmer.scheme.controller.AdminSchemeController;
import com.farmer.scheme.entity.Scheme;
import com.farmer.scheme.entity.Scheme.SchemeType;
import com.farmer.scheme.repository.SchemeRepository;
import com.farmer.scheme.service.SchemeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AdminSchemeController.
 * Requirements: 4.1, 4.2, 4.3, 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7, 5.8, 5.9
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminSchemeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SchemeRepository schemeRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Scheme testScheme;

    @BeforeEach
    void setUp() {
        schemeRepository.deleteAll();
        
        testScheme = Scheme.builder()
                .schemeCode("PM-KISAN-001")
                .schemeName("PM-Kisan Samman Nidhi")
                .schemeType(SchemeType.CENTRAL)
                .description("Income support of Rs. 6,000 per year to farmer families")
                .eligibilityCriteria("{\"landholding\": \"any\", \"income\": \"less than 3 lakh/year\"}")
                .benefitAmount(new BigDecimal("6000.00"))
                .benefitDescription("Rs. 6,000 per year in three installments of Rs. 2,000 each")
                .applicationStartDate(LocalDate.now().minusDays(30))
                .applicationEndDate(LocalDate.now().plusDays(60))
                .applicationUrl("https://pmkisan.gov.in")
                .contactInfo("{\"phone\": \"011-23381092\", \"email\": \"pmkisan@gov.in\"}")
                .isActive(true)
                .build();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldCreateSchemeAsAdmin() throws Exception {
        mockMvc.perform(post("/api/v1/admin/schemes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testScheme)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schemeCode", is("PM-KISAN-001")))
                .andExpect(jsonPath("$.schemeName", is("PM-Kisan Samman Nidhi")))
                .andExpect(jsonPath("$.schemeType", is("CENTRAL")));
    }

    @Test
    @WithMockUser(roles = "FARMER")
    void shouldDenyAccessToFarmerForAdminEndpoints() throws Exception {
        mockMvc.perform(post("/api/v1/admin/schemes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testScheme)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldDenyAccessWithoutAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/admin/schemes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testScheme)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldUpdateSchemeAsAdmin() throws Exception {
        Scheme savedScheme = schemeRepository.save(testScheme);
        
        Scheme updateDetails = Scheme.builder()
                .id(savedScheme.getId())
                .schemeCode(savedScheme.getSchemeCode())
                .schemeName("Updated PM-Kisan Scheme")
                .schemeType(savedScheme.getSchemeType())
                .benefitAmount(new BigDecimal("8000.00"))
                .isActive(true)
                .build();

        mockMvc.perform(put("/api/v1/admin/schemes/" + savedScheme.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schemeName", is("Updated PM-Kisan Scheme")))
                .andExpect(jsonPath("$.benefitAmount", is(8000.00)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldDeactivateSchemeAsAdmin() throws Exception {
        Scheme savedScheme = schemeRepository.save(testScheme);
        
        mockMvc.perform(delete("/api/v1/admin/schemes/" + savedScheme.getId()))
                .andExpect(status().isNoContent());
        
        // Verify scheme is deactivated
        Scheme deactivatedScheme = schemeRepository.findById(savedScheme.getId()).orElseThrow();
        assert !deactivatedScheme.getIsActive();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldActivateDeactivatedScheme() throws Exception {
        testScheme.setIsActive(false);
        Scheme savedScheme = schemeRepository.save(testScheme);
        
        mockMvc.perform(put("/api/v1/admin/schemes/" + savedScheme.getId() + "/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive", is(true)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldGetAllSchemesIncludingInactive() throws Exception {
        Scheme activeScheme = schemeRepository.save(testScheme);
        
        Scheme inactiveScheme = Scheme.builder()
                .schemeCode("INACTIVE-001")
                .schemeName("Inactive Scheme")
                .schemeType(SchemeType.STATE)
                .state("Maharashtra")
                .isActive(false)
                .build();
        schemeRepository.save(inactiveScheme);
        
        mockMvc.perform(get("/api/v1/admin/schemes/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldGetSchemeStatistics() throws Exception {
        // Create schemes of different types
        schemeRepository.save(testScheme); // CENTRAL
        
        Scheme stateScheme = Scheme.builder()
                .schemeCode("MAHA-001")
                .schemeName("Maharashtra Scheme")
                .schemeType(SchemeType.STATE)
                .state("Maharashtra")
                .isActive(true)
                .build();
        schemeRepository.save(stateScheme);
        
        Scheme insuranceScheme = Scheme.builder()
                .schemeCode("PMFBY-001")
                .schemeName("PM Fasal Bima Yojana")
                .schemeType(SchemeType.INSURANCE)
                .isActive(true)
                .build();
        schemeRepository.save(insuranceScheme);

        mockMvc.perform(get("/api/v1/admin/schemes/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSchemes", is(3)))
                .andExpect(jsonPath("$.activeSchemes", is(3)))
                .andExpect(jsonPath("$.centralSchemes", is(1)))
                .andExpect(jsonPath("$.stateSchemes", is(1)))
                .andExpect(jsonPath("$.insuranceSchemes", is(1)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturnNotFoundWhenUpdatingNonExistentScheme() throws Exception {
        Scheme updateDetails = Scheme.builder()
                .schemeName("Updated Scheme")
                .build();

        mockMvc.perform(put("/api/v1/admin/schemes/99999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDetails)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldCreateStateSpecificScheme() throws Exception {
        Scheme karnatakaScheme = Scheme.builder()
                .schemeCode("KAR-001")
                .schemeName("Karnataka Krishi Bhagya")
                .schemeType(SchemeType.STATE)
                .state("Karnataka")
                .description("Micro-irrigation scheme for Karnataka farmers")
                .benefitAmount(new BigDecimal("50000.00"))
                .isActive(true)
                .build();

        mockMvc.perform(post("/api/v1/admin/schemes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(karnatakaScheme)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schemeCode", is("KAR-001")))
                .andExpect(jsonPath("$.state", is("Karnataka")))
                .andExpect(jsonPath("$.schemeType", is("STATE")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldCreateCropSpecificScheme() throws Exception {
        Scheme cropScheme = Scheme.builder()
                .schemeCode("PADDY-001")
                .schemeName("Paddy Cultivation Support")
                .schemeType(SchemeType.CROP_SPECIFIC)
                .applicableCrops("Paddy,Rice")
                .description("Support scheme for paddy cultivators")
                .benefitAmount(new BigDecimal("10000.00"))
                .isActive(true)
                .build();

        mockMvc.perform(post("/api/v1/admin/schemes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cropScheme)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schemeType", is("CROP_SPECIFIC")))
                .andExpect(jsonPath("$.applicableCrops", containsString("Paddy")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldCreateInsuranceScheme() throws Exception {
        Scheme insuranceScheme = Scheme.builder()
                .schemeCode("INS-001")
                .schemeName("Crop Insurance Scheme")
                .schemeType(SchemeType.INSURANCE)
                .description("Comprehensive crop insurance coverage")
                .subsidyPercentage(new BigDecimal("2.00"))
                .maxBenefitAmount(new BigDecimal("500000.00"))
                .isActive(true)
                .build();

        mockMvc.perform(post("/api/v1/admin/schemes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(insuranceScheme)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schemeType", is("INSURANCE")))
                .andExpect(jsonPath("$.subsidyPercentage", is(2.00)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldCreateSubsidyScheme() throws Exception {
        Scheme subsidyScheme = Scheme.builder()
                .schemeCode("SUB-001")
                .schemeName("Micro-Irrigation Subsidy")
                .schemeType(SchemeType.SUBSIDY)
                .subsidyPercentage(new BigDecimal("55.00"))
                .description("55% subsidy on micro-irrigation systems")
                .targetBeneficiaries("Small and marginal farmers")
                .isActive(true)
                .build();

        mockMvc.perform(post("/api/v1/admin/schemes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(subsidyScheme)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schemeType", is("SUBSIDY")))
                .andExpect(jsonPath("$.subsidyPercentage", is(55.00)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldCreateWelfareScheme() throws Exception {
        Scheme welfareScheme = Scheme.builder()
                .schemeCode("WEL-001")
                .schemeName("Kisan Maan Dhan Yojana")
                .schemeType(SchemeType.WELFARE)
                .description("Pension scheme for farmers")
                .benefitAmount(new BigDecimal("3000.00"))
                .landholdingRequirement("Up to 2 hectares")
                .isActive(true)
                .build();

        mockMvc.perform(post("/api/v1/admin/schemes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(welfareScheme)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schemeType", is("WELFARE")));
    }
}