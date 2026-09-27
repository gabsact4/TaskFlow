package com.KernelPanic.TaskFlow.repository;

import com.KernelPanic.TaskFlow.entity.WebAuthnChallenge;
import com.KernelPanic.TaskFlow.enums.WebAuthnCeremony;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

import java.time.Instant;
import java.util.Optional;

public interface WebAuthnChallengeRepository extends JpaRepository<WebAuthnChallenge, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<WebAuthnChallenge> findByIdAndCeremonyAndExpiresAtAfter(
            String id, WebAuthnCeremony ceremony, Instant now);

    long deleteByExpiresAtBefore(Instant now);
}