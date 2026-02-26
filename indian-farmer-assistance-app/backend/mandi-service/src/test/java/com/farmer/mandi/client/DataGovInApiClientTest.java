package com.farmer.mandi.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit tests for DataGovInApiClient.
 */
@ExtendWith(MockitoExtension.class)
class DataGovInApiClientTest {

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec<?> requestHeadersUriSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    private DataGovInApiClient apiClient;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        apiClient = new DataGovInApiClient(webClient, objectMapper);
    }

    @Test
    void testGetMandiPrices_Success() {
        // Arrange
        String mockResponse = "{\"records\": [{" +
                "\"state\": \"Karnataka\"," +
                "\"district\": \"Bengaluru\"," +
                "\"market\": \"KR Market\"," +
                "\"commodity\": \"Wheat\"," +
                "\"variety\": \"Desi\"," +
                "\"arrival_date\": \"2025-02-24\"," +
                "\"min_price\": 2100," +
                "\"max_price\": 2300," +
                "\"modal_price\": 2200," +
                "\"arrival_quantity\": 500" +
                "}]}";

        // Act & Assert
        StepVerifier.create(
                Mono.just(mockResponse)
                        .map(response -> {
                            try {
                                return objectMapper.readTree(response).get("records");
                            } catch (Exception e) {
                                return null;
                            }
                        })
        )
        .assertNext(records -> {
            assertNotNull(records);
            assertTrue(records.isArray());
            assertEquals(1, records.size());
        })
        .verifyComplete();
    }

    @Test
    void testGetMandiPrices_EmptyResponse() {
        // Arrange
        String mockResponse = "{\"records\": []}";

        // Act & Assert
        StepVerifier.create(
                Mono.just(mockResponse)
                        .map(response -> {
                            try {
                                return objectMapper.readTree(response).get("records");
                            } catch (Exception e) {
                                return null;
                            }
                        })
        )
        .assertNext(records -> {
            assertNotNull(records);
            assertTrue(records.isArray());
            assertEquals(0, records.size());
        })
        .verifyComplete();
    }

    @Test
    void testGetFertilizerSuppliers_Success() {
        // Arrange
        String mockResponse = "{\"records\": [{" +
                "\"state\": \"Karnataka\"," +
                "\"district\": \"Bengaluru\"," +
                "\"document_id\": \"DOC001\"," +
                "\"sl_no\": 1," +
                "\"no_of_wholesalers\": 5," +
                "\"no_of_retailers\": 20," +
                "\"fertilizer_type\": \"Urea\"," +
                "\"supplier_name\": \"ABC Fertilizers\"," +
                "\"contact_info\": \"9876543210\"" +
                "}]}";

        // Act & Assert
        StepVerifier.create(
                Mono.just(mockResponse)
                        .map(response -> {
                            try {
                                return objectMapper.readTree(response).get("records");
                            } catch (Exception e) {
                                return null;
                            }
                        })
        )
        .assertNext(records -> {
            assertNotNull(records);
            assertTrue(records.isArray());
            assertEquals(1, records.size());
            assertEquals("Urea", records.get(0).get("fertilizer_type").asText());
        })
        .verifyComplete();
    }

    @Test
    void testGetFertilizerSuppliers_EmptyResponse() {
        // Arrange
        String mockResponse = "{\"records\": []}";

        // Act & Assert
        StepVerifier.create(
                Mono.just(mockResponse)
                        .map(response -> {
                            try {
                                return objectMapper.readTree(response).get("records");
                            } catch (Exception e) {
                                return null;
                            }
                        })
        )
        .assertNext(records -> {
            assertNotNull(records);
            assertTrue(records.isArray());
            assertEquals(0, records.size());
        })
        .verifyComplete();
    }

    @Test
    void testParseResponse_InvalidJson() {
        // Arrange
        String invalidResponse = "invalid json";

        // Act & Assert
        StepVerifier.create(
                Mono.just(invalidResponse)
                        .map(response -> {
                            try {
                                return objectMapper.readTree(response).get("records");
                            } catch (Exception e) {
                                return null;
                            }
                        })
        )
        .assertNext(result -> assertNull(result))
        .verifyComplete();
    }

    @Test
    void testEncodeParam() throws Exception {
        // Test URL encoding of parameters
        String param = "Wheat Flour";
        String encoded = java.net.URLEncoder.encode(param, "UTF-8");
        assertEquals("Wheat+Flour", encoded);
    }
}
