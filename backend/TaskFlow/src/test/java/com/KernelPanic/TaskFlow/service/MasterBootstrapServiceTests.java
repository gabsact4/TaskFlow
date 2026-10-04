package com.KernelPanic.TaskFlow.service;

import com.KernelPanic.TaskFlow.dto.AuthResponse;
import com.KernelPanic.TaskFlow.dto.RegisterRequest;
import com.KernelPanic.TaskFlow.entity.User;
import com.KernelPanic.TaskFlow.enums.Role;
import com.KernelPanic.TaskFlow.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MasterBootstrapServiceTests {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private UserRepository userRepository;

    @Mock
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Mock
    private AuthService authService;

    @InjectMocks
    private MasterBootstrapService masterBootstrapService;

    @BeforeEach
    void configureSecret() {
        ReflectionTestUtils.setField(masterBootstrapService, "configuredToken", "a-secure-bootstrap-token-with-32chars");
        System.setProperty("DATA_ENCRYPTION_KEY", "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");
    }

    @AfterEach
    void clearTestKey() {
        System.clearProperty("DATA_ENCRYPTION_KEY");
    }

    @Test
    void createsFirstMasterAndConsumesBootstrapToken() {
        RegisterRequest request = new RegisterRequest("Initial Master", "master@example.com", "a-long-bootstrap-password");
        when(userRepository.existsByRoleIn(List.of(Role.MASTER, Role.ADMIN))).thenReturn(false);
        when(jdbcTemplate.update(anyString())).thenReturn(1);
        when(userRepository.existsByEmailLookupHash(anyString())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("hashed-password");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        AuthResponse expected = new AuthResponse("token", 3600, null);
        when(authService.issueTokenFor(any(User.class))).thenReturn(expected);

        AuthResponse response = masterBootstrapService.bootstrap(request, "a-secure-bootstrap-token-with-32chars");

        assertEquals(expected, response);
        ArgumentCaptor<User> user = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(user.capture());
        assertEquals(Role.MASTER, user.getValue().getRole());
        assertEquals("master@example.com", user.getValue().getEmail());
        verify(jdbcTemplate).update(eq("UPDATE master_bootstrap_state SET consumed = TRUE WHERE id = 1 AND consumed = FALSE"));
    }

    @Test
    void rejectsInvalidSecretBeforeTouchingBootstrapState() {
        RegisterRequest request = new RegisterRequest("Master", "master@example.com", "a-long-bootstrap-password");

        assertThrows(ResponseStatusException.class,
                () -> masterBootstrapService.bootstrap(request, "incorrect-token"));

        verify(jdbcTemplate, never()).update(anyString());
        verify(userRepository, never()).saveAndFlush(any(User.class));
    }
}
