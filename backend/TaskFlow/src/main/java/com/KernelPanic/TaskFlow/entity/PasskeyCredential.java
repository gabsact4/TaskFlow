package com.KernelPanic.TaskFlow.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "passkey_credentials")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasskeyCredential {

    @Id
    @Column(name = "credential_id", nullable = false, length = 1024, columnDefinition = "VARBINARY(1024)")
    private byte[] credentialId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "user_handle", nullable = false, length = 64)
    private String userHandle;

    @Column(name = "public_key_cose", nullable = false, length = 4096)
    private String publicKeyCose;

    @Column(name = "signature_count", nullable = false)
    private long signatureCount;
}