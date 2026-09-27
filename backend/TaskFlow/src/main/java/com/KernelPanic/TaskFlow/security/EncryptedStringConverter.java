package com.KernelPanic.TaskFlow.security;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private static final String PREFIX = "enc:v1:";
    private static final int NONCE_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public String convertToDatabaseColumn(String value) {
        if (value == null) {
            return null;
        }

        byte[] nonce = new byte[NONCE_LENGTH];
        RANDOM.nextBytes(nonce);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey(), new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
            cipher.updateAAD(PREFIX.getBytes(StandardCharsets.UTF_8));
            byte[] ciphertext = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[nonce.length + ciphertext.length];
            System.arraycopy(nonce, 0, payload, 0, nonce.length);
            System.arraycopy(ciphertext, 0, payload, nonce.length, ciphertext.length);
            return PREFIX + Base64.getEncoder().encodeToString(payload);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Não foi possível criptografar o dado", exception);
        }
    }

    @Override
    public String convertToEntityAttribute(String value) {
        if (value == null || !value.startsWith(PREFIX)) {
            return value;
        }

        try {
            byte[] payload = Base64.getDecoder().decode(value.substring(PREFIX.length()));
            if (payload.length <= NONCE_LENGTH) {
                throw new IllegalArgumentException("Conteúdo criptografado inválido");
            }
            byte[] nonce = Arrays.copyOfRange(payload, 0, NONCE_LENGTH);
            byte[] ciphertext = Arrays.copyOfRange(payload, NONCE_LENGTH, payload.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey(), new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
            cipher.updateAAD(PREFIX.getBytes(StandardCharsets.UTF_8));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new IllegalStateException("Não foi possível descriptografar o dado", exception);
        }
    }

    private SecretKeySpec encryptionKey() {
        String encodedKey = System.getProperty("DATA_ENCRYPTION_KEY");
        if (encodedKey == null || encodedKey.isBlank()) {
            encodedKey = System.getenv("DATA_ENCRYPTION_KEY");
        }
        if (encodedKey == null || encodedKey.isBlank()) {
            throw new IllegalStateException("A variável DATA_ENCRYPTION_KEY precisa estar configurada");
        }

        try {
            byte[] key = Base64.getDecoder().decode(encodedKey);
            if (key.length != 32) {
                throw new IllegalStateException("DATA_ENCRYPTION_KEY deve conter uma chave Base64 de 32 bytes");
            }
            return new SecretKeySpec(key, "AES");
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("DATA_ENCRYPTION_KEY deve ser Base64 válido", exception);
        }
    }
}