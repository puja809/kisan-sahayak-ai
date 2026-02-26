package com.farmer.common.security;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Service for AES-256 encryption and decryption of sensitive data at rest.
 * Implements DPDP Act 2023 compliance for data protection.
 */
public class EncryptionService {

    private static final String ALGORITHM = "AES";
    private static final int KEY_SIZE = 256;
    private final SecretKey secretKey;

    public EncryptionService(String keyString) throws Exception {
        byte[] decodedKey = Base64.getDecoder().decode(keyString);
        this.secretKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, ALGORITHM);
    }

    /**
     * Generate a new AES-256 encryption key
     */
    public static String generateKey() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
        keyGenerator.init(KEY_SIZE, new SecureRandom());
        SecretKey key = keyGenerator.generateKey();
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    /**
     * Encrypt sensitive data using AES-256
     */
    public String encrypt(String plaintext) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encryptedData = cipher.doFinal(plaintext.getBytes());
        return Base64.getEncoder().encodeToString(encryptedData);
    }

    /**
     * Decrypt AES-256 encrypted data
     */
    public String decrypt(String encryptedText) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] decodedData = Base64.getDecoder().decode(encryptedText);
        byte[] decryptedData = cipher.doFinal(decodedData);
        return new String(decryptedData);
    }
}
