package com.KernelPanic.TaskFlow.controller;

import com.KernelPanic.TaskFlow.dto.AuthResponse;
import com.KernelPanic.TaskFlow.dto.LoginRequest;
import com.KernelPanic.TaskFlow.dto.RegisterRequest;
import com.KernelPanic.TaskFlow.service.AuthService;
import com.KernelPanic.TaskFlow.service.MasterBootstrapService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints públicos de autenticação por e-mail e senha.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final MasterBootstrapService masterBootstrapService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/bootstrap-master")
    public ResponseEntity<AuthResponse> bootstrapMaster(
            @Valid @RequestBody RegisterRequest request,
            @RequestHeader("X-TaskFlow-Bootstrap-Token") String token) {
        return ResponseEntity.status(HttpStatus.CREATED).body(masterBootstrapService.bootstrap(request, token));
    }
}
