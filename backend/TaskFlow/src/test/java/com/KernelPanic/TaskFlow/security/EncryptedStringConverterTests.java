package com.KernelPanic.TaskFlow.security;

import java.util.Base64;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

class EncryptedStringConverterTests {

    private final EncryptedStringConverter converter = new EncryptedStringConverter();
    private final String previousKey = System.getProperty("DATA_ENCRYPTION_KEY");

    @AfterEach
    void restoreEncryptionKey() {
        if (previousKey == null) {
            System.clearProperty("DATA_ENCRYPTION_KEY");
        } else {
            System.setProperty("DATA_ENCRYPTION_KEY", previousKey);
        }
    }

    @Test
    void encryptsWithRandomNonceAndDecryptsValue() {
        System.setProperty("DATA_ENCRYPTION_KEY", Base64.getEncoder().encodeToString(new byte[32]));

        String first = converter.convertToDatabaseColumn("Nome da pessoa");
        String second = converter.convertToDatabaseColumn("Nome da pessoa");

        assertNotEquals("Nome da pessoa", first);
        assertNotEquals(first, second);
        assertEquals("Nome da pessoa", converter.convertToEntityAttribute(first));
    }

    @Test
    void readsLegacyPlaintextValues() {
        assertEquals("Nome legado", converter.convertToEntityAttribute("Nome legado"));
    }

    @Test
    void emailLookupHashIsStableAndNormalized() {
        System.setProperty("DATA_ENCRYPTION_KEY", Base64.getEncoder().encodeToString(new byte[32]));

        assertEquals(EmailLookupHash.of("User@Example.com"), EmailLookupHash.of(" user@example.com "));
        assertNotEquals(EmailLookupHash.of("user@example.com"), EmailLookupHash.of("other@example.com"));
    }

    @Test
    void rejectsTamperedCiphertext() {
        System.setProperty("DATA_ENCRYPTION_KEY", Base64.getEncoder().encodeToString(new byte[32]));
        String encrypted = converter.convertToDatabaseColumn("Nome");
        String tampered = encrypted.substring(0, encrypted.length() - 2) + "AA";

        assertThrows(IllegalStateException.class, () -> converter.convertToEntityAttribute(tampered));
    }
}