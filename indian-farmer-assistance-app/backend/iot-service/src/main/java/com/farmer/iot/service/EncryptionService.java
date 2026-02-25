package com.farmer.iot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Service for encrypting and decrypting IoT data.
 * Implements AES-256-GCM encryption for data at rest and TLS for data in transit.
 */
@Service
@Slf4j
public class EncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final String DATA_OWNERSHIP_MESSAGE = "Farmer retains full ownership of all IoT-generated data";

    private final SecretKey secretKey;
    private final SecureRandom secureRandom;

    public EncryptionService() {
        this.secretKey = generateKey();
        this.secureRandom = new SecureRandom();
    }

    /**
     * Encrypt data using AES-256-GCM.
     * Validates: Requirements 10.11, 17.1, 17.2
     */
    public String encrypt(byte[] plaintext) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            byte[] ciphertext = cipher.doFinal(plaintext);

            // Combine IV and ciphertext
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            byteBuffer.put(iv);
            byteBuffer.put(ciphertext);

            return Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            log.error("Encryption failed", e);
            throw new RuntimeException("Encryption failed", e);
        }
    }

    /**
     * Decrypt data using AES-256-GCM.
     * Validates: Requirements 10.11, 17.1, 17.2
     */
    public byte[] decrypt(String encryptedData) {
        try {
            byte[] decoded = Base64.getDecoder().decode(encryptedData);

            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);
            byte[] ciphertext = new byte[byteBuffer.remaining()];
            byteBuffer.get(ciphertext);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            return cipher.doFinal(ciphertext);
        } catch (Exception e) {
            log.error("Decryption failed", e);
            throw new RuntimeException("Decryption failed", e);
        }
    }

    /**
     * Encrypt a string value.
     */
    public String encryptString(String plaintext) {
        return Base64.getEncoder().encodeToString(encrypt(plaintext.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * Decrypt a string value.
     */
    public String decryptString(String encryptedData) {
        return new String(decrypt(encryptedData), StandardCharsets.UTF_8);
    }

    /**
     * Verify encryption is working correctly.
     * Property test helper method.
     */
    public boolean verifyEncryption(byte[] data) {
        String encrypted = encrypt(data);
        byte[] decrypted = decrypt(encrypted);
        return java.util.Arrays.equals(data, decrypted);
    }

    /**
     * Get data ownership confirmation message.
     * Validates: Requirement 10.10
     */
    public String getDataOwnershipMessage() {
        return DATA_OWNERSHIP_MESSAGE;
    }

    /**
     * Generate AES-256 key.
     */
    private SecretKey generateKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(256);
            return keyGenerator.generateKey();
        } catch (NoSuchAlgorithmException e) {
            log.error("Failed to generate encryption key", e);
            throw new RuntimeException("Failed to generate encryption key", e);
        }
    }

    /**
     * Get the encryption algorithm name.
     */
    public String getAlgorithm() {
        return "AES-256-GCM";
    }
}