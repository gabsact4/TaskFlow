package com.KernelPanic.TaskFlow.service;

import com.KernelPanic.TaskFlow.entity.WebAuthnChallenge;
import com.KernelPanic.TaskFlow.enums.WebAuthnCeremony;
import com.KernelPanic.TaskFlow.repository.WebAuthnChallengeRepository;
import com.KernelPanic.TaskFlow.exception.InvalidWebAuthnException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WebAuthnChallengeService {

    private final WebAuthnChallengeRepository challengeRepository;

    @Transactional
    public String create(WebAuthnCeremony ceremony, Long userId, String requestJson) {
        Instant now = Instant.now();
        challengeRepository.deleteByExpiresAtBefore(now);
        String id = UUID.randomUUID().toString();
        challengeRepository.save(WebAuthnChallenge.builder()
                .id(id)
                .userId(userId)
                .ceremony(ceremony)
                .requestJson(requestJson)
                .expiresAt(now.plus(5, ChronoUnit.MINUTES))
                .build());
        return id;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public WebAuthnChallenge consume(String id, WebAuthnCeremony ceremony) {
        WebAuthnChallenge challenge = challengeRepository
                .findByIdAndCeremonyAndExpiresAtAfter(id, ceremony, Instant.now())
                .orElseThrow(InvalidWebAuthnException::new);
        challengeRepository.delete(challenge);
        return challenge;
    }
}