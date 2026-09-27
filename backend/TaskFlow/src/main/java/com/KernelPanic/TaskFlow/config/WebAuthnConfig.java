package com.KernelPanic.TaskFlow.config;

import com.KernelPanic.TaskFlow.security.WebAuthnCredentialRepositoryAdapter;
import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.data.RelyingPartyIdentity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
public class WebAuthnConfig {

    @Bean
    public RelyingParty relyingParty(
            WebAuthnCredentialRepositoryAdapter credentialRepository,
            @Value("${security.webauthn.rp-id}") String relyingPartyId,
            @Value("${security.webauthn.rp-name}") String relyingPartyName,
            @Value("${security.webauthn.origins}") String allowedOrigins) {
        RelyingPartyIdentity identity = RelyingPartyIdentity.builder()
                .id(relyingPartyId)
                .name(relyingPartyName)
                .build();
        Set<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
        if (origins.isEmpty()) {
            throw new IllegalStateException("Configure ao menos uma origem para WebAuthn");
        }
        return RelyingParty.builder()
                .identity(identity)
                .credentialRepository(credentialRepository)
                .origins(origins)
                .validateSignatureCounter(true)
                .build();
    }
}