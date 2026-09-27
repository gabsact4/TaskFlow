package com.KernelPanic.TaskFlow.controller;

import com.KernelPanic.TaskFlow.dto.AuthResponse;
import com.KernelPanic.TaskFlow.dto.WebAuthnFinishRequest;
import com.KernelPanic.TaskFlow.dto.WebAuthnOptionsResponse;
import com.KernelPanic.TaskFlow.service.WebAuthnService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/passkeys")
@RequiredArgsConstructor
public class WebAuthnController {

    private final WebAuthnService webAuthnService;

    @PostMapping("/registration/options")
    public WebAuthnOptionsResponse startRegistration(@AuthenticationPrincipal UserDetails principal) {
        return webAuthnService.startRegistration(principal.getUsername());
    }

    @PostMapping("/registration/verify")
    public ResponseEntity<Void> finishRegistration(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody WebAuthnFinishRequest request) {
        webAuthnService.finishRegistration(principal.getUsername(), request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/login/options")
    public WebAuthnOptionsResponse startAuthentication() {
        return webAuthnService.startAuthentication();
    }

    @PostMapping("/login/verify")
    public AuthResponse finishAuthentication(@Valid @RequestBody WebAuthnFinishRequest request) {
        return webAuthnService.finishAuthentication(request);
    }
}