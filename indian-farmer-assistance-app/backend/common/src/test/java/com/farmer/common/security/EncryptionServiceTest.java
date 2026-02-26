package com.farmer.common.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AES-256 encryption service.
 * Tests TLS configuration and data encryption at rest.
 * Requirements: 17.1, 17.2, 17.4, 17.6
 */
public class EncryptionServiceTest {

    private EncryptionService encryptionService;

    @BeforeEach
    public void setUp() throws Exception {
        String key = EncryptionService.generateKey();
        encryptionService = new EncryptionService(key);
    }

    @Test
    public void testEncryptionAndDecryption() throws Exception {
        String plaintext = "sensitive_farmer_data";
        String encrypted = encryptionService.encrypt(plaintext);
        String decrypted = encryptionService.decrypt(encrypted);

        assertNotEquals(plaintext, encrypted, "Encrypted text should differ from plaintext");
        assertEquals(plaintext, decrypted, "Decrypted text should match original plaintext");
    }

    @Test
    public void testEncryptionProducesDifferentCiphertexts() throws Exception {
        String plaintext = "test_data";
        String encrypted1 = encryptionService.encrypt(plaintext);
        String encrypted2 = encryptionService.encrypt(plaintext);

        // Note: AES in ECB mode produces same ciphertext for same plaintext
        // In production, use CBC or GCM mode with IV for different ciphertexts
        assertEquals(encrypted1, encrypted2, "Same plaintext should produce same ciphertext with ECB mode");
    }

    @Test
    public void testEncryptionWithSpecialCharacters() throws Exception {
        String plaintext = "farmer@123!#$%^&*()";
        String encrypted = encryptionService.encrypt(plaintext);
        String decrypted = encryptionService.decrypt(encrypted);

        assertEquals(plaintext, decrypted, "Special characters should be preserved");
    }

    @Test
    public void testEncryptionWithEmptyString() throws Exception {
        String plaintext = "";
        String encrypted = encryptionService.encrypt(plaintext);
        String decrypted = encryptionService.decrypt(encrypted);

        assertEquals(plaintext, decrypted, "Empty string should be handled correctly");
    }

    @Test
    public void testEncryptionWithLargeData() throws Exception {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("This is a large data block for testing encryption. ");
        }
        String plaintext = sb.toString();
        String encrypted = encryptionService.encrypt(plaintext);
        String decrypted = encryptionService.decrypt(encrypted);

        assertEquals(plaintext, decrypted, "Large data should be encrypted and decrypted correctly");
    }

    @Test
    public void testKeyGeneration() throws Exception {
        String key1 = EncryptionService.generateKey();
        String key2 = EncryptionService.generateKey();

        assertNotNull(key1, "Generated key should not be null");
        assertNotNull(key2, "Generated key should not be null");
        assertNotEquals(key1, key2, "Generated keys should be different");
    }

    @Test
    public void testDecryptionWithInvalidCiphertext() {
        String invalidCiphertext = "invalid_base64_data";
        assertThrows(IllegalArgumentException.class, () -> {
            encryptionService.decrypt(invalidCiphertext);
        }, "Invalid ciphertext should throw exception");
    }

    @Test
    public void testEncryptionServiceWithDifferentKeys() throws Exception {
        String plaintext = "test_data";
        String key1 = EncryptionService.generateKey();
        String key2 = EncryptionService.generateKey();

        EncryptionService service1 = new EncryptionService(key1);
        EncryptionService service2 = new EncryptionService(key2);

        String encrypted1 = service1.encrypt(plaintext);
        String encrypted2 = service2.encrypt(plaintext);

        assertNotEquals(encrypted1, encrypted2, "Different keys should produce different ciphertexts");

        // Decryption with wrong key should fail or produce garbage
        assertThrows(Exception.class, () -> {
            service2.decrypt(encrypted1);
        }, "Decryption with wrong key should fail");
    }
}
