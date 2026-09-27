package com.KernelPanic.TaskFlow.security;

import com.KernelPanic.TaskFlow.entity.PasskeyCredential;
import com.KernelPanic.TaskFlow.repository.PasskeyCredentialRepository;
import com.KernelPanic.TaskFlow.repository.UserRepository;
import com.yubico.webauthn.CredentialRepository;
import com.yubico.webauthn.RegisteredCredential;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.PublicKeyCredentialDescriptor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Base64;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class WebAuthnCredentialRepositoryAdapter implements CredentialRepository {

    private final PasskeyCredentialRepository credentialRepository;
    private final UserRepository userRepository;

    @Override
    public Set<PublicKeyCredentialDescriptor> getCredentialIdsForUsername(String username) {
        return userRepository.findByEmailLookupHash(EmailLookupHash.of(username))
                .map(user -> credentialRepository.findAllByUserId(user.getId()).stream()
                        .map(credential -> PublicKeyCredentialDescriptor.builder()
                                .id(new ByteArray(credential.getCredentialId()))
                                .build())
                        .collect(Collectors.toSet()))
                .orElseGet(Set::of);
    }

    @Override
    public Optional<ByteArray> getUserHandleForUsername(String username) {
        return userRepository.findByEmailLookupHash(EmailLookupHash.of(username))
                .map(user -> user.getWebauthnUserHandle())
                .filter(handle -> handle != null)
                .map(handle -> new ByteArray(Base64.getUrlDecoder().decode(handle)));
    }

    @Override
    public Optional<String> getUsernameForUserHandle(ByteArray userHandle) {
        return userRepository.findByWebauthnUserHandle(userHandle.getBase64Url())
                .map(user -> user.getEmail());
    }

    @Override
    public Optional<RegisteredCredential> lookup(ByteArray credentialId, ByteArray userHandle) {
        return credentialRepository.findByCredentialIdAndUserHandle(
                        credentialId.getBytes(), userHandle.getBase64Url())
                .map(this::toRegisteredCredential);
    }

    @Override
    public Set<RegisteredCredential> lookupAll(ByteArray credentialId) {
        List<PasskeyCredential> credentials = credentialRepository.findAllByCredentialId(credentialId.getBytes());
        return credentials.stream().map(this::toRegisteredCredential).collect(Collectors.toSet());
    }

    private RegisteredCredential toRegisteredCredential(PasskeyCredential credential) {
        return RegisteredCredential.builder()
                .credentialId(new ByteArray(credential.getCredentialId()))
            .userHandle(new ByteArray(Base64.getUrlDecoder().decode(credential.getUserHandle())))
            .publicKeyCose(new ByteArray(Base64.getUrlDecoder().decode(credential.getPublicKeyCose())))
                .signatureCount(credential.getSignatureCount())
                .build();
    }
}