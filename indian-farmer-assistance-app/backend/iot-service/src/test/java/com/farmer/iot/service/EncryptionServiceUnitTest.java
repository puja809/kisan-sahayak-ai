package com.farmer.iot.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for data encryption service.
 * Validates: Requirements 10.10, 10.11, 17.1, 17.2
 */
class EncryptionServiceUnitTest {

    private EncryptionService encryptionService;

    @BeforeEach
    void setUp() {
        encryptionService = new EncryptionService();
    }

    @Test
    void encrypt_shouldReturnEncryptedData() {
        // Given
        byte[] plaintext = "Test sensor data".getBytes(StandardCharsets.UTF_8);

        // When
        String encrypted = encryptionService.encrypt(plaintext);

        // Then
        assertNotNull(encrypted);
        assertNotEquals(new String(plaintext, StandardCharsets.UTF_8), encrypted);
    }

    @Test
    void decrypt_shouldReturnOriginalData() {
        // Given
        byte[] plaintext = "Test sensor data".getBytes(StandardCharsets.UTF_8);
        String encrypted = encryptionService.encrypt(plaintext);

        // When
        byte[] decrypted = encryptionService.decrypt(encrypted);

        // Then
        assertNotNull(decrypted);
        assertArrayEquals(plaintext, decrypted);
    }

    @Test
    void encryptString_shouldReturnEncryptedString() {
        // Given
        String plaintext = "Soil moisture: 45%";

        // When
        String encrypted = encryptionService.encryptString(plaintext);

        // Then
        assertNotNull(encrypted);
        assertNotEquals(plaintext, encrypted);
    }

    @Test
    void decryptString_shouldReturnOriginalString() {
        // Given
        String plaintext = "Soil moisture: 45%";
        String encrypted = encryptionService.encryptString(plaintext);

        // When
        String decrypted = encryptionService.decryptString(encrypted);

        // Then
        assertEquals(plaintext, decrypted);
    }

    @Test
    void verifyEncryption_shouldReturnTrueForValidData() {
        // Given
        byte[] data = "Sensor reading data".getBytes(StandardCharsets.UTF_8);

        // When
        boolean result = encryptionService.verifyEncryption(data);

        // Then
        assertTrue(result);
    }

    @Test
    void verifyEncryption_shouldReturnFalseForDifferentData() {
        // Given
        byte[] originalData = "Original data".getBytes(StandardCharsets.UTF_8);
        byte[] differentData = "Different data".getBytes(StandardCharsets.UTF_8);

        // When
        String encrypted = encryptionService.encrypt(originalData);
        byte[] decrypted = encryptionService.decrypt(encrypted);

        // Then
        assertFalse(Arrays.equals(differentData, decrypted));
    }

    @Test
    void encrypt_shouldProduceDifferentCiphertextForSamePlaintext() {
        // Given
        byte[] plaintext = "Test data".getBytes(StandardCharsets.UTF_8);

        // When
        String encrypted1 = encryptionService.encrypt(plaintext);
        String encrypted2 = encryptionService.encrypt(plaintext);

        // Then - Due to random IV, encrypted data should be different
        assertNotEquals(encrypted1, encrypted2);
    }

    @Test
    void decrypt_shouldHandleEmptyData() {
        // Given
        byte[] emptyData = new byte[0];
        String encrypted = encryptionService.encrypt(emptyData);

        // When
        byte[] decrypted = encryptionService.decrypt(encrypted);

        // Then
        assertEquals(0, decrypted.length);
    }

    @Test
    void decrypt_shouldHandleLargeData() {
        // Given
        byte[] largeData = new byte[10000];
        Arrays.fill(largeData, (byte) 'A');
        String encrypted = encryptionService.encrypt(largeData);

        // When
        byte[] decrypted = encryptionService.decrypt(encrypted);

        // Then
        assertArrayEquals(largeData, decrypted);
    }

    @Test
    void getDataOwnershipMessage_shouldReturnMessage() {
        // When
        String message = encryptionService.getDataOwnershipMessage();

        // Then
        assertNotNull(message);
        assertTrue(message.contains("Farmer retains full ownership"));
    }

    @Test
    void getAlgorithm_shouldReturnAes256Gcm() {
        // When
        String algorithm = encryptionService.getAlgorithm();

        // Then
        assertEquals("AES-256-GCM", algorithm);
    }

    @Test
    void encrypt_shouldHandleSpecialCharacters() {
        // Given
        byte[] specialData = "Special chars: @#$%^&*()_+{}|:<>?".getBytes(StandardCharsets.UTF_8);

        // When
        String encrypted = encryptionService.encrypt(specialData);
        byte[] decrypted = encryptionService.decrypt(encrypted);

        // Then
        assertArrayEquals(specialData, decrypted);
    }

    @Test
    void encrypt_shouldHandleUnicodeCharacters() {
        // Given
        byte[] unicodeData = "Unicode: 你好世界 🌍".getBytes(StandardCharsets.UTF_8);

        // When
        String encrypted = encryptionService.encrypt(unicodeData);
        byte[] decrypted = encryptionService.decrypt(encrypted);

        // Then
        assertArrayEquals(unicodeData, decrypted);
    }
}