package com.farmer.mandi.service;

import com.farmer.mandi.client.DataGovInApiClient;
import com.farmer.mandi.dto.FertilizerSupplierDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit tests for FertilizerSupplierService.
 */
@ExtendWith(MockitoExtension.class)
class FertilizerSupplierServiceTest {

    @Mock
    private DataGovInApiClient dataGovInApiClient;

    private FertilizerSupplierService fertilizerSupplierService;

    @BeforeEach
    void setUp() {
        fertilizerSupplierService = new FertilizerSupplierService(dataGovInApiClient);
    }

    @Test
    void testGetSuppliersByLocation_Success() {
        // Arrange
        List<DataGovInApiClient.FertilizerSupplierRecord> mockRecords = new ArrayList<>();
        mockRecords.add(DataGovInApiClient.FertilizerSupplierRecord.builder()
                .state("Karnataka")
                .district("Bengaluru")
                .documentId("DOC001")
                .slNo(1)
                .noOfWholesalers(5)
                .noOfRetailers(20)
                .fertilizerType("Urea")
                .supplierName("ABC Fertilizers")
                .contactInfo("9876543210")
                .build());

        when(dataGovInApiClient.getFertilizerSuppliers("Karnataka", "Bengaluru", 0, 100))
                .thenReturn(Mono.just(mockRecords));

        // Act & Assert
        StepVerifier.create(fertilizerSupplierService.getSuppliersByLocation("Karnataka", "Bengaluru"))
                .assertNext(suppliers -> {
                    assertNotNull(suppliers);
                    assertEquals(1, suppliers.size());
                    assertEquals("Urea", suppliers.get(0).getFertilizerType());
                    assertEquals("ABC Fertilizers", suppliers.get(0).getSupplierName());
                })
                .verifyComplete();
    }

    @Test
    void testGetSuppliersByLocation_EmptyResult() {
        // Arrange
        when(dataGovInApiClient.getFertilizerSuppliers("Karnataka", "Bengaluru", 0, 100))
                .thenReturn(Mono.just(new ArrayList<>()));

        // Act & Assert
        StepVerifier.create(fertilizerSupplierService.getSuppliersByLocation("Karnataka", "Bengaluru"))
                .assertNext(suppliers -> {
                    assertNotNull(suppliers);
                    assertEquals(0, suppliers.size());
                })
                .verifyComplete();
    }

    @Test
    void testGetSuppliersByState_Success() {
        // Arrange
        List<DataGovInApiClient.FertilizerSupplierRecord> mockRecords = new ArrayList<>();
        mockRecords.add(DataGovInApiClient.FertilizerSupplierRecord.builder()
                .state("Karnataka")
                .district("Bengaluru")
                .noOfWholesalers(5)
                .noOfRetailers(20)
                .fertilizerType("DAP")
                .supplierName("XYZ Suppliers")
                .contactInfo("9876543211")
                .build());

        when(dataGovInApiClient.getFertilizerSuppliers("Karnataka", null, 0, 100))
                .thenReturn(Mono.just(mockRecords));

        // Act & Assert
        StepVerifier.create(fertilizerSupplierService.getSuppliersByState("Karnataka"))
                .assertNext(suppliers -> {
                    assertNotNull(suppliers);
                    assertEquals(1, suppliers.size());
                    assertEquals("DAP", suppliers.get(0).getFertilizerType());
                })
                .verifyComplete();
    }

    @Test
    void testGetSuppliersByWholesalers_SortedCorrectly() {
        // Arrange
        List<DataGovInApiClient.FertilizerSupplierRecord> mockRecords = new ArrayList<>();
        mockRecords.add(DataGovInApiClient.FertilizerSupplierRecord.builder()
                .state("Karnataka")
                .district("Bengaluru")
                .noOfWholesalers(10)
                .noOfRetailers(20)
                .supplierName("Supplier A")
                .build());
        mockRecords.add(DataGovInApiClient.FertilizerSupplierRecord.builder()
                .state("Karnataka")
                .district("Bengaluru")
                .noOfWholesalers(5)
                .noOfRetailers(15)
                .supplierName("Supplier B")
                .build());

        when(dataGovInApiClient.getFertilizerSuppliers("Karnataka", "Bengaluru", 0, 100))
                .thenReturn(Mono.just(mockRecords));

        // Act & Assert
        StepVerifier.create(fertilizerSupplierService.getSuppliersByWholesalers("Karnataka", "Bengaluru"))
                .assertNext(suppliers -> {
                    assertNotNull(suppliers);
                    assertEquals(2, suppliers.size());
                    // Should be sorted by wholesalers descending
                    assertEquals(10, suppliers.get(0).getNoOfWholesalers());
                    assertEquals(5, suppliers.get(1).getNoOfWholesalers());
                })
                .verifyComplete();
    }

    @Test
    void testGetSuppliersByRetailers_SortedCorrectly() {
        // Arrange
        List<DataGovInApiClient.FertilizerSupplierRecord> mockRecords = new ArrayList<>();
        mockRecords.add(DataGovInApiClient.FertilizerSupplierRecord.builder()
                .state("Karnataka")
                .district("Bengaluru")
                .noOfWholesalers(5)
                .noOfRetailers(30)
                .supplierName("Supplier A")
                .build());
        mockRecords.add(DataGovInApiClient.FertilizerSupplierRecord.builder()
                .state("Karnataka")
                .district("Bengaluru")
                .noOfWholesalers(10)
                .noOfRetailers(15)
                .supplierName("Supplier B")
                .build());

        when(dataGovInApiClient.getFertilizerSuppliers("Karnataka", "Bengaluru", 0, 100))
                .thenReturn(Mono.just(mockRecords));

        // Act & Assert
        StepVerifier.create(fertilizerSupplierService.getSuppliersByRetailers("Karnataka", "Bengaluru"))
                .assertNext(suppliers -> {
                    assertNotNull(suppliers);
                    assertEquals(2, suppliers.size());
                    // Should be sorted by retailers descending
                    assertEquals(30, suppliers.get(0).getNoOfRetailers());
                    assertEquals(15, suppliers.get(1).getNoOfRetailers());
                })
                .verifyComplete();
    }

    @Test
    void testGetSuppliersByFertilizerType_FilteredCorrectly() {
        // Arrange
        List<DataGovInApiClient.FertilizerSupplierRecord> mockRecords = new ArrayList<>();
        mockRecords.add(DataGovInApiClient.FertilizerSupplierRecord.builder()
                .state("Karnataka")
                .district("Bengaluru")
                .fertilizerType("Urea")
                .supplierName("Supplier A")
                .build());
        mockRecords.add(DataGovInApiClient.FertilizerSupplierRecord.builder()
                .state("Karnataka")
                .district("Bengaluru")
                .fertilizerType("DAP")
                .supplierName("Supplier B")
                .build());

        when(dataGovInApiClient.getFertilizerSuppliers("Karnataka", "Bengaluru", 0, 100))
                .thenReturn(Mono.just(mockRecords));

        // Act & Assert
        StepVerifier.create(fertilizerSupplierService.getSuppliersByFertilizerType("Karnataka", "Bengaluru", "Urea"))
                .assertNext(suppliers -> {
                    assertNotNull(suppliers);
                    assertEquals(1, suppliers.size());
                    assertEquals("Urea", suppliers.get(0).getFertilizerType());
                })
                .verifyComplete();
    }
}
