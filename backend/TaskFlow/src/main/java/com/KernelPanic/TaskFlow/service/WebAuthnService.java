package com.KernelPanic.TaskFlow.service;

import com.KernelPanic.TaskFlow.dto.AuthResponse;
import com.KernelPanic.TaskFlow.dto.WebAuthnFinishRequest;
import com.KernelPanic.TaskFlow.dto.WebAuthnOptionsResponse;
import com.KernelPanic.TaskFlow.entity.PasskeyCredential;
import com.KernelPanic.TaskFlow.entity.User;
import com.KernelPanic.TaskFlow.entity.WebAuthnChallenge;
import com.KernelPanic.TaskFlow.enums.WebAuthnCeremony;
import com.KernelPanic.TaskFlow.exception.InvalidWebAuthnException;
import com.KernelPanic.TaskFlow.repository.PasskeyCredentialRepository;
import com.KernelPanic.TaskFlow.repository.UserRepository;
import com.KernelPanic.TaskFlow.security.EmailLookupHash;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yubico.webauthn.AssertionRequest;
import com.yubico.webauthn.AssertionResult;
import com.yubico.webauthn.FinishAssertionOptions;
import com.yubico.webauthn.FinishRegistrationOptions;
import com.yubico.webauthn.RegistrationResult;
import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.StartAssertionOptions;
import com.yubico.webauthn.StartRegistrationOptions;
import com.yubico.webauthn.data.AuthenticatorAssertionResponse;
import com.yubico.webauthn.data.AuthenticatorSelectionCriteria;
import com.yubico.webauthn.data.ClientAssertionExtensionOutputs;
import com.yubico.webauthn.data.ClientRegistrationExtensionOutputs;
import com.yubico.webauthn.data.PublicKeyCredential;
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions;
import com.yubico.webauthn.data.ResidentKeyRequirement;
import com.yubico.webauthn.data.UserIdentity;
import com.yubico.webauthn.data.UserVerificationRequirement;
import com.yubico.webauthn.data.ByteArray;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WebAuthnService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final ObjectMapper JSON = new ObjectMapper();

    private final RelyingParty relyingParty;
    private final UserRepository userRepository;
    private final PasskeyCredentialRepository credentialRepository;
    private final WebAuthnChallengeService challengeService;
    private final AuthService authService;

    @Transactional
    public WebAuthnOptionsResponse startRegistration(String email) {
        User user = userRepository.findByEmailLookupHash(EmailLookupHash.of(email))
                .orElseThrow(InvalidWebAuthnException::new);
        if (user.getWebauthnUserHandle() == null) {
            byte[] handle = new byte[32];
            RANDOM.nextBytes(handle);
            user.setWebauthnUserHandle(Base64.getUrlEncoder().withoutPadding().encodeToString(handle));
            userRepository.save(user);
        }

        PublicKeyCredentialCreationOptions options = relyingParty.startRegistration(
                StartRegistrationOptions.builder()
                        .user(UserIdentity.builder()
                                .name(user.getEmail())
                                .displayName(user.getName())
                                .id(new ByteArray(Base64.getUrlDecoder().decode(user.getWebauthnUserHandle())))
                                .build())
                        .authenticatorSelection(AuthenticatorSelectionCriteria.builder()
                                .residentKey(ResidentKeyRequirement.REQUIRED)
                                .userVerification(UserVerificationRequirement.REQUIRED)
                                .build())
                        .build());
        try {
            String challengeId = challengeService.create(
                    WebAuthnCeremony.REGISTRATION, user.getId(), options.toJson());
            return new WebAuthnOptionsResponse(challengeId, parseOptions(options.toCredentialsCreateJson()));
        } catch (Exception exception) {
            throw new IllegalStateException("Não foi possível iniciar o cadastro da passkey", exception);
        }
    }

    public WebAuthnOptionsResponse startAuthentication() {
        AssertionRequest request = relyingParty.startAssertion(StartAssertionOptions.builder()
                .userVerification(UserVerificationRequirement.REQUIRED)
                .build());
        try {
            String challengeId = challengeService.create(
                WebAuthnCeremony.AUTHENTICATION, null, request.toJson());
            return new WebAuthnOptionsResponse(challengeId, parseOptions(request.toCredentialsGetJson()));
        } catch (Exception exception) {
            throw new IllegalStateException("Não foi possível iniciar a autenticação por passkey", exception);
        }
    }

    public void finishRegistration(String email, WebAuthnFinishRequest request) {
        WebAuthnChallenge challenge = challengeService.consume(request.challengeId(), WebAuthnCeremony.REGISTRATION);
        User user = userRepository.findByEmailLookupHash(EmailLookupHash.of(email))
                .orElseThrow(InvalidWebAuthnException::new);
        if (!user.getId().equals(challenge.getUserId())) {
            throw new InvalidWebAuthnException();
        }

        try {
            PublicKeyCredentialCreationOptions creationOptions =
                    PublicKeyCredentialCreationOptions.fromJson(challenge.getRequestJson());
            PublicKeyCredential<
                    com.yubico.webauthn.data.AuthenticatorAttestationResponse,
                    ClientRegistrationExtensionOutputs> response =
                    PublicKeyCredential.parseRegistrationResponseJson(request.credential());
            RegistrationResult result = relyingParty.finishRegistration(FinishRegistrationOptions.builder()
                    .request(creationOptions)
                    .response(response)
                    .build());
            credentialRepository.save(PasskeyCredential.builder()
                    .credentialId(result.getKeyId().getId().getBytes())
                    .userId(user.getId())
                    .userHandle(user.getWebauthnUserHandle())
                    .publicKeyCose(result.getPublicKeyCose().getBase64Url())
                    .signatureCount(result.getSignatureCount())
                    .build());
        } catch (Exception exception) {
            throw new InvalidWebAuthnException();
        }
    }

    public AuthResponse finishAuthentication(WebAuthnFinishRequest request) {
        WebAuthnChallenge challenge = challengeService.consume(request.challengeId(), WebAuthnCeremony.AUTHENTICATION);
        try {
            AssertionRequest assertionRequest = AssertionRequest.fromJson(challenge.getRequestJson());
            PublicKeyCredential<AuthenticatorAssertionResponse, ClientAssertionExtensionOutputs> response =
                    PublicKeyCredential.parseAssertionResponseJson(request.credential());
            AssertionResult result = relyingParty.finishAssertion(FinishAssertionOptions.builder()
                    .request(assertionRequest)
                    .response(response)
                    .build());
            if (!result.isSuccess() || !result.isUserVerified()) {
                throw new InvalidWebAuthnException();
            }

            PasskeyCredential credential = credentialRepository.findById(
                            result.getCredential().getCredentialId().getBytes())
                    .orElseThrow(InvalidWebAuthnException::new);
            credential.setSignatureCount(result.getSignatureCount());
            credentialRepository.save(credential);
            User user = userRepository.findById(credential.getUserId())
                    .orElseThrow(InvalidWebAuthnException::new);
            return authService.issueTokenFor(user);
        } catch (InvalidWebAuthnException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new InvalidWebAuthnException();
        }
    }

    private Map<String, Object> parseOptions(String json) throws Exception {
        return JSON.readValue(json, new TypeReference<>() {
        });
    }
}