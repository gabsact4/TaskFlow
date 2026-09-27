package com.KernelPanic.TaskFlow.entity;

import com.KernelPanic.TaskFlow.enums.WebAuthnCeremony;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "webauthn_challenges")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebAuthnChallenge {

    @Id
    @Column(name = "challenge_id", nullable = false, length = 36)
    private String id;

    @Column(name = "user_id")
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WebAuthnCeremony ceremony;

    @Column(name = "request_json", nullable = false, columnDefinition = "TEXT")
    private String requestJson;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
}