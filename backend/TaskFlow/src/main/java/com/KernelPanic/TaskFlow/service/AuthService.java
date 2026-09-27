package com.KernelPanic.TaskFlow.service;

import com.KernelPanic.TaskFlow.dto.AuthResponse;
import com.KernelPanic.TaskFlow.dto.LoginRequest;
import com.KernelPanic.TaskFlow.dto.RegisterRequest;
import com.KernelPanic.TaskFlow.dto.UserResponse;
import com.KernelPanic.TaskFlow.entity.User;
import com.KernelPanic.TaskFlow.enums.Role;
import com.KernelPanic.TaskFlow.exception.EmailAlreadyExistsException;
import com.KernelPanic.TaskFlow.repository.UserRepository;
import com.KernelPanic.TaskFlow.security.JwtService;
import com.KernelPanic.TaskFlow.security.EmailLookupHash;
import org.springframework.dao.DataIntegrityViolationException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Locale;

/**
 * Camada de aplicação responsável pelas regras de negócio de autenticação:
 * cadastro de novos usuários e emissão de token na autenticação (login).
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = normalize(request.email());

        if (userRepository.existsByEmailLookupHash(EmailLookupHash.of(normalizedEmail))) {
            throw new EmailAlreadyExistsException(normalizedEmail);
        }

        User user = User.builder()
                .name(request.name().trim())
                .email(normalizedEmail)
                .emailLookupHash(EmailLookupHash.of(normalizedEmail))
                .password(passwordEncoder.encode(request.password()))
                .role(Role.USER)
                .build();

        try {
            User saved = userRepository.saveAndFlush(user);
            return buildAuthResponse(saved);
        } catch (DataIntegrityViolationException exception) {
            throw new EmailAlreadyExistsException(normalizedEmail);
        }
    }

    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = normalize(request.email());

        // Delega a verificação de credenciais ao AuthenticationManager/DaoAuthenticationProvider,
        // que por sua vez usa o PasswordEncoder para comparar o hash com a senha informada.
        // Lança BadCredentialsException (tratada globalmente) em caso de falha.
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedEmail, request.password())
        );

        User user = userRepository.findByEmailLookupHash(EmailLookupHash.of(normalizedEmail))
                .orElseThrow(() -> new IllegalStateException(
                        "Usuário autenticado não encontrado após validação de credenciais"));

        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        String token = jwtService.generateToken(user);
        long expiresInSeconds = jwtService.getExpirationMs() / 1000;
        return new AuthResponse(token, expiresInSeconds, UserResponse.fromEntity(user));
    }

    public AuthResponse issueTokenFor(User user) {
        return buildAuthResponse(user);
    }

    private String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
