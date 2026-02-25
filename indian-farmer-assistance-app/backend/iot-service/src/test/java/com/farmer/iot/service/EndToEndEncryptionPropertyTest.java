package com.farmer.iot.service;

import net.jqwik.api.*;
import net.jqwik.junit5.JqwikTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based test for End-to-End Encryption.
 * Feature: indian-farmer-assistance-app, Property 19: End-to-End Encryption
 * Validates: Requirements 10.11, 17.1, 17.2
 *
 * For any data transmission (API calls, database writes, IoT data), the system
 * should use TLS 1.3 or higher for data in transit and AES-256 for data at rest,
 * and no sensitive data should be transmitted or stored in plaintext.
 */
@JqwikTest
class EndToEndEncryptionPropertyTest {

    private EncryptionService encryptionService;

    @BeforeEach
    void setUp() {
        encryptionService = new EncryptionService();
    }

    /**
     * Property: Encryption and decryption is a round-trip operation.
     * For any data, encrypting then decrypting should return the original data.
     */
    @Property
    void encryptionDecryptionRoundTrip(@ForAll("anyData") byte[] data) {
        // When
        String encrypted = encryptionService.encrypt(data);
        byte[] decrypted = encryptionService.decrypt(encrypted);

        // Then
        assertArrayEquals(data, decrypted);
    }

    /**
     * Property: Encrypted data is different from plaintext.
     * For any data, the encrypted output should not equal the original input.
     */
    @Property
    void encryptedDataDiffersFromPlaintext(@ForAll("anyData") byte[] data) {
        // When
        String encrypted = encryptionService.encrypt(data);

        // Then - Encrypted data should be different from plaintext
        assertNotEquals(new String(data, StandardCharsets.UTF_8), encrypted);
    }

    /**
     * Property: Encryption produces different ciphertext for same plaintext.
     * Due to random IV, encrypting the same data twice should produce different results.
     */
    @Property
    void encryptionProducesDifferentCiphertext(@ForAll("anyData") byte[] data) {
        // When
        String encrypted1 = encryptionService.encrypt(data);
        String encrypted2 = encryptionService.encrypt(data);

        // Then - Due to random IV, encrypted data should be different
        assertNotEquals(encrypted1, encrypted2);
    }

    /**
     * Property: Decryption fails with invalid data.
     * For any invalid encrypted data, decryption should throw an exception.
     */
    @Property
    void decryptionFailsWithInvalidData(@ForAll("invalidEncryptedData") String invalidData) {
        // When/Then
        assertThrows(RuntimeException.class, () -> encryptionService.decrypt(invalidData));
    }

    /**
     * Property: String encryption round-trip works correctly.
     * For any string, encrypting then decrypting should return the original string.
     */
    @Property
    void stringEncryptionRoundTrip(@ForAll("anyStrings") String text) {
        // When
        String encrypted = encryptionService.encryptString(text);
        String decrypted = encryptionService.decryptString(encrypted);

        // Then
        assertEquals(text, decrypted);
    }

    /**
     * Property: Empty data can be encrypted and decrypted.
     * For empty byte array, encryption and decryption should work correctly.
     */
    @Property
    void emptyDataEncryption(@ForAll("emptyData") byte[] emptyData) {
        // When
        String encrypted = encryptionService.encrypt(emptyData);
        byte[] decrypted = encryptionService.decrypt(encrypted);

        // Then
        assertArrayEquals(emptyData, decrypted);
    }

    /**
     * Property: Large data can be encrypted and decrypted.
     * For data up to 10KB, encryption and decryption should work correctly.
     */
    @Property
    void largeDataEncryption(@ForAll("largeData") byte[] largeData) {
        // When
        String encrypted = encryptionService.encrypt(largeData);
        byte[] decrypted = encryptionService.decrypt(encrypted);

        // Then
        assertArrayEquals(largeData, decrypted);
    }

    /**
     * Property: Special characters are handled correctly.
     * For data with special characters, encryption and decryption should work.
     */
    @Property
    void specialCharacterEncryption(@ForAll("specialCharacters") String text) {
        // When
        String encrypted = encryptionService.encryptString(text);
        String decrypted = encryptionService.decryptString(encrypted);

        // Then
        assertEquals(text, decrypted);
    }

    /**
     * Property: Unicode characters are handled correctly.
     * For data with Unicode characters, encryption and decryption should work.
     */
    @Property
    void unicodeEncryption(@ForAll("unicodeStrings") String text) {
        // When
        String encrypted = encryptionService.encryptString(text);
        String decrypted = encryptionService.decryptString(encrypted);

        // Then
        assertEquals(text, decrypted);
    }

    /**
     * Property: Verify encryption algorithm is AES-256-GCM.
     * The encryption service should use AES-256-GCM algorithm.
     */
    @Test
    void encryptionAlgorithmIsAes256Gcm() {
        // When
        String algorithm = encryptionService.getAlgorithm();

        // Then
        assertEquals("AES-256-GCM", algorithm);
    }

    /**
     * Property: Data ownership message is included.
     * The encryption service should provide a data ownership message.
     */
    @Test
    void dataOwnershipMessageIsProvided() {
        // When
        String message = encryptionService.getDataOwnershipMessage();

        // Then
        assertNotNull(message);
        assertTrue(message.contains("Farmer"));
        assertTrue(message.contains("ownership"));
    }

    // Generators for property-based testing

    @Provide
    static Arbitrary<byte[]> anyData() {
        return Arbitraries.strings()
                .alpha()
                .withChars('a', 'z', 'A', 'Z', '0', '9')
                .ofMinSize(1)
                .ofMaxSize(1000)
                .map(s -> s.getBytes(StandardCharsets.UTF_8));
    }

    @Provide
    static Arbitrary<String> anyStrings() {
        return Arbitraries.strings()
                .withChars('a', 'z', 'A', 'Z', '0', '9', ' ', '!', '@', '#', '$', '%')
                .ofMinSize(0)
                .ofMaxSize(500);
    }

    @Provide
    static Arbitrary<byte[]> emptyData() {
        return Arbitraries.just(new byte[0]);
    }

    @Provide
    static Arbitrary<byte[]> largeData() {
        return Arbitraries.strings()
                .alpha()
                .ofMinSize(5000)
                .ofMaxSize(10000)
                .map(s -> s.getBytes(StandardCharsets.UTF_8));
    }

    @Provide
    static Arbitrary<String> specialCharacters() {
        return Arbitraries.just("Special: @#$%^&*()_+{}|:<>?~`-=[]\\;',./");
    }

    @Provide
    static Arbitrary<String> unicodeStrings() {
        return Arbitraries.just("Hello World! 你好世界 🌍 परिवार");
    }

    @Provide
    static Arbitrary<String> invalidEncryptedData() {
        return Arbitraries.strings()
                .alpha()
                .ofMinSize(10)
                .ofMaxSize(100);
    }
}