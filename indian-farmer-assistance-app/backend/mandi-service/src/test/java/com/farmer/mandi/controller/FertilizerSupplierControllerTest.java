package com.farmer.mandi.controller;

import com.farmer.mandi.dto.FertilizerSupplierDto;
import com.farmer.mandi.service.FertilizerSupplierService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit tests for FertilizerSupplierController.
 */
@ExtendWith(MockitoExtension.class)
class FertilizerSupplierControllerTest {

    @Mock
    private FertilizerSupplierService fertilizerSupplierService;

    private FertilizerSupplierController controller;

    @BeforeEach
    void setUp() {
        controller = new FertilizerSupplierController(fertilizerSupplierService);
    }

    @Test
    void testGetSuppliersByLocation_Success() {
        // Arrange
        List<FertilizerSupplierDto> mockSuppliers = new ArrayList<>();
        mockSuppliers.add(FertilizerSupplierDto.builder()
                .state("Karnataka")
                .district("Bengaluru")
                .supplierName("ABC Fertilizers")
                .noOfWholesalers(5)
                .noOfRetailers(20)
                .build());

        when(fertilizerSupplierService.getSuppliersByLocation("Karnataka", "Bengaluru"))
                .thenReturn(Mono.just(mockSuppliers));

        // Act & Assert
        StepVerifier.create(controller.getSuppliersByLocation("Karnataka", "Bengaluru"))
                .assertNext(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    assertNotNull(response.getBody());
                    assertEquals(1, response.getBody().size());
                })
                .verifyComplete();
    }

    @Test
    void testGetSuppliersByLocation_EmptyResult() {
        // Arrange
        when(fertilizerSupplierService.getSuppliersByLocation("Karnataka", "Bengaluru"))
                .thenReturn(Mono.just(new ArrayList<>()));

        // Act & Assert
        StepVerifier.create(controller.getSuppliersByLocation("Karnataka", "Bengaluru"))
                .assertNext(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    assertNotNull(response.getBody());
                    assertEquals(0, response.getBody().size());
                })
                .verifyComplete();
    }

    @Test
    void testGetSuppliersByState_Success() {
        // Arrange
        List<FertilizerSupplierDto> mockSuppliers = new ArrayList<>();
        mockSuppliers.add(FertilizerSupplierDto.builder()
                .state("Karnataka")
                .district("Bengaluru")
                .supplierName("XYZ Suppliers")
                .build());

        when(fertilizerSupplierService.getSuppliersByState("Karnataka"))
                .thenReturn(Mono.just(mockSuppliers));

        // Act & Assert
        StepVerifier.create(controller.getSuppliersByState("Karnataka"))
                .assertNext(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    assertNotNull(response.getBody());
                    assertEquals(1, response.getBody().size());
                })
                .verifyComplete();
    }

    @Test
    void testGetSuppliers_WithPagination() {
        // Arrange
        List<FertilizerSupplierDto> mockSuppliers = new ArrayList<>();
        mockSuppliers.add(FertilizerSupplierDto.builder()
                .state("Karnataka")
                .district("Bengaluru")
                .supplierName("Supplier A")
                .build());

        when(fertilizerSupplierService.getSuppliers("Karnataka", "Bengaluru", 0, 50))
                .thenReturn(Mono.just(mockSuppliers));

        // Act & Assert
        StepVerifier.create(controller.getSuppliers("Karnataka", "Bengaluru", 0, 50))
                .assertNext(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    assertNotNull(response.getBody());
                    assertEquals(1, response.getBody().size());
                })
                .verifyComplete();
    }

    @Test
    void testGetSuppliersByWholesalers_Success() {
        // Arrange
        List<FertilizerSupplierDto> mockSuppliers = new ArrayList<>();
        mockSuppliers.add(FertilizerSupplierDto.builder()
                .state("Karnataka")
                .district("Bengaluru")
                .supplierName("Supplier A")
                .noOfWholesalers(10)
                .build());

        when(fertilizerSupplierService.getSuppliersByWholesalers("Karnataka", "Bengaluru"))
                .thenReturn(Mono.just(mockSuppliers));

        // Act & Assert
        StepVerifier.create(controller.getSuppliersByWholesalers("Karnataka", "Bengaluru"))
                .assertNext(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    assertNotNull(response.getBody());
                    assertEquals(1, response.getBody().size());
                })
                .verifyComplete();
    }

    @Test
    void testGetSuppliersByRetailers_Success() {
        // Arrange
        List<FertilizerSupplierDto> mockSuppliers = new ArrayList<>();
        mockSuppliers.add(FertilizerSupplierDto.builder()
                .state("Karnataka")
                .district("Bengaluru")
                .supplierName("Supplier A")
                .noOfRetailers(20)
                .build());

        when(fertilizerSupplierService.getSuppliersByRetailers("Karnataka", "Bengaluru"))
                .thenReturn(Mono.just(mockSuppliers));

        // Act & Assert
        StepVerifier.create(controller.getSuppliersByRetailers("Karnataka", "Bengaluru"))
                .assertNext(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    assertNotNull(response.getBody());
                    assertEquals(1, response.getBody().size());
                })
                .verifyComplete();
    }

    @Test
    void testGetSuppliersByFertilizerType_Success() {
        // Arrange
        List<FertilizerSupplierDto> mockSuppliers = new ArrayList<>();
        mockSuppliers.add(FertilizerSupplierDto.builder()
                .state("Karnataka")
                .district("Bengaluru")
                .fertilizerType("Urea")
                .supplierName("Supplier A")
                .build());

        when(fertilizerSupplierService.getSuppliersByFertilizerType("Karnataka", "Bengaluru", "Urea"))
                .thenReturn(Mono.just(mockSuppliers));

        // Act & Assert
        StepVerifier.create(controller.getSuppliersByFertilizerType("Karnataka", "Bengaluru", "Urea"))
                .assertNext(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    assertNotNull(response.getBody());
                    assertEquals(1, response.getBody().size());
                })
                .verifyComplete();
    }

    @Test
    void testGetSuppliersByLocation_Error() {
        // Arrange
        when(fertilizerSupplierService.getSuppliersByLocation("Karnataka", "Bengaluru"))
                .thenReturn(Mono.error(new RuntimeException("API Error")));

        // Act & Assert
        StepVerifier.create(controller.getSuppliersByLocation("Karnataka", "Bengaluru"))
                .assertNext(response -> {
                    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
                })
                .verifyComplete();
    }
}
