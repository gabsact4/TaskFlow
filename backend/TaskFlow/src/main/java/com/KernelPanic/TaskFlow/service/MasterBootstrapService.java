package com.KernelPanic.TaskFlow.service;

import com.KernelPanic.TaskFlow.dto.AuthResponse;
import com.KernelPanic.TaskFlow.dto.RegisterRequest;
import com.KernelPanic.TaskFlow.entity.User;
import com.KernelPanic.TaskFlow.enums.Role;
import com.KernelPanic.TaskFlow.exception.EmailAlreadyExistsException;
import com.KernelPanic.TaskFlow.repository.UserRepository;
import com.KernelPanic.TaskFlow.security.EmailLookupHash;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Locale;

/** One-time, secret-protected Master bootstrap for installations with existing Dev accounts. */
@Service
@RequiredArgsConstructor
public class MasterBootstrapService {

    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;

    @Value("${TASKFLOW_BOOTSTRAP_MASTER_TOKEN:}")
    private String configuredToken;

    @Transactional
    public AuthResponse bootstrap(RegisterRequest request, String suppliedToken) {
        if (configuredToken.length() < 32 || suppliedToken == null
                || !MessageDigest.isEqual(configuredToken.getBytes(StandardCharsets.UTF_8),
                        suppliedToken.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Token de bootstrap inválido ou não configurado.");
        }
        if (request.password().length() < 12) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A senha inicial do Master deve ter pelo menos 12 caracteres.");
        }
        if (userRepository.existsByRoleIn(List.of(Role.MASTER, Role.ADMIN))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe um usuário Master configurado.");
        }

        int claimed = jdbcTemplate.update(
                "UPDATE master_bootstrap_state SET consumed = TRUE WHERE id = 1 AND consumed = FALSE");
        if (claimed != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "O bootstrap do Master já foi utilizado.");
        }

        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailLookupHash(EmailLookupHash.of(email))) {
            throw new EmailAlreadyExistsException(email);
        }
        User master = User.builder()
                .name(request.name().trim())
                .email(email)
                .emailLookupHash(EmailLookupHash.of(email))
                .password(passwordEncoder.encode(request.password()))
                .role(Role.MASTER)
                .build();
        return authService.issueTokenFor(userRepository.saveAndFlush(master));
    }
}
