package com.KernelPanic.TaskFlow.repository;

import com.KernelPanic.TaskFlow.entity.PasskeyCredential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PasskeyCredentialRepository extends JpaRepository<PasskeyCredential, byte[]> {

    List<PasskeyCredential> findAllByUserId(Long userId);

    List<PasskeyCredential> findAllByCredentialId(byte[] credentialId);

    java.util.Optional<PasskeyCredential> findByCredentialIdAndUserHandle(byte[] credentialId, String userHandle);
}