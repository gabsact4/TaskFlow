package com.KernelPanic.TaskFlow.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

public final class EmailLookupHash {

    private static final byte[] DOMAIN = "TaskFlow email lookup v1".getBytes(StandardCharsets.UTF_8);

    private EmailLookupHash() {
    }

    public static String of(String email) {
        try {
            String encodedKey = System.getProperty("DATA_ENCRYPTION_KEY");
            if (encodedKey == null || encodedKey.isBlank()) {
                encodedKey = System.getenv("DATA_ENCRYPTION_KEY");
            }
            if (encodedKey == null || encodedKey.isBlank()) {
                throw new IllegalStateException("A variável DATA_ENCRYPTION_KEY precisa estar configurada");
            }
            byte[] key = Base64.getDecoder().decode(encodedKey);
            if (key.length != 32) {
                throw new IllegalStateException("DATA_ENCRYPTION_KEY deve conter uma chave Base64 de 32 bytes");
            }

            Mac keyDerivation = Mac.getInstance("HmacSHA256");
            keyDerivation.init(new SecretKeySpec(key, "HmacSHA256"));
            byte[] lookupKey = keyDerivation.doFinal(DOMAIN);
            Mac lookup = Mac.getInstance("HmacSHA256");
            lookup.init(new SecretKeySpec(lookupKey, "HmacSHA256"));
            byte[] digest = lookup.doFinal(email.trim().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new IllegalStateException("Não foi possível gerar o índice de busca do e-mail", exception);
        }
    }
}