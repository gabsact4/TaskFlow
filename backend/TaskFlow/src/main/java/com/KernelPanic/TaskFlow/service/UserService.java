package com.KernelPanic.TaskFlow.service;

import com.KernelPanic.TaskFlow.dto.AssignableUserResponse;
import com.KernelPanic.TaskFlow.dto.ChangePasswordRequest;
import com.KernelPanic.TaskFlow.dto.CreateUserRequest;
import com.KernelPanic.TaskFlow.dto.PageResponse;
import com.KernelPanic.TaskFlow.dto.UpdateProfileRequest;
import com.KernelPanic.TaskFlow.dto.UpdateRoleRequest;
import com.KernelPanic.TaskFlow.dto.UserResponse;
import com.KernelPanic.TaskFlow.entity.User;
import com.KernelPanic.TaskFlow.enums.Role;
import com.KernelPanic.TaskFlow.exception.EmailAlreadyExistsException;
import com.KernelPanic.TaskFlow.exception.InvalidCurrentPasswordException;
import com.KernelPanic.TaskFlow.exception.UserNotFoundException;
import com.KernelPanic.TaskFlow.repository.UserRepository;
import com.KernelPanic.TaskFlow.security.EmailLookupHash;
import org.springframework.dao.DataIntegrityViolationException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Locale;
import java.util.List;

/**
 * Camada de aplicação responsável pelo gerenciamento de usuários já cadastrados:
 * consulta, atualização de perfil, troca de senha, alteração de papel (role)
 * e exclusão de conta.
 * <p>
 * A autorização (quem pode chamar cada operação) é resolvida no
 * {@code UserController} via {@code @PreAuthorize}; este serviço assume que
 * a chamada já foi autorizada.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (!List.of(Role.DEV, Role.PO, Role.MASTER).contains(request.role())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Papel inválido para uma nova conta.");
        }
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailLookupHash(EmailLookupHash.of(normalizedEmail))) {
            throw new EmailAlreadyExistsException(normalizedEmail);
        }
        User user = User.builder()
                .name(request.name().trim())
                .email(normalizedEmail)
                .emailLookupHash(EmailLookupHash.of(normalizedEmail))
                .password(passwordEncoder.encode(request.password()))
                .role(request.role())
                .build();
        try {
            return UserResponse.fromEntity(userRepository.saveAndFlush(user));
        } catch (DataIntegrityViolationException exception) {
            throw new EmailAlreadyExistsException(normalizedEmail);
        }
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        return UserResponse.fromEntity(findUserOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<AssignableUserResponse> listAssignableUsers() {
        return userRepository.findAll().stream().map(AssignableUserResponse::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> listUsers(Pageable pageable) {
        Page<UserResponse> page = userRepository.findAll(pageable).map(UserResponse::fromEntity);
        return PageResponse.from(page);
    }

    @Transactional
    public UserResponse updateProfile(Long id, UpdateProfileRequest request) {
        User user = findUserOrThrow(id);
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);

        boolean emailChanged = !normalizedEmail.equals(user.getEmail());
        if (emailChanged && userRepository.existsByEmailLookupHash(EmailLookupHash.of(normalizedEmail))) {
            throw new EmailAlreadyExistsException(normalizedEmail);
        }

        user.setName(request.name().trim());
        user.setEmail(normalizedEmail);
        user.setEmailLookupHash(EmailLookupHash.of(normalizedEmail));

        try {
            return UserResponse.fromEntity(userRepository.saveAndFlush(user));
        } catch (DataIntegrityViolationException exception) {
            throw new EmailAlreadyExistsException(normalizedEmail);
        }
    }

    @Transactional
    public void changePassword(Long id, ChangePasswordRequest request) {
        User user = findUserOrThrow(id);

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new InvalidCurrentPasswordException();
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    @Transactional
    public int setReminderDays(Long id, int days) {
        if (days < 0 || days > 30) throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST, "O lembrete deve ser entre 0 e 30 dias.");
        User user = findUserOrThrow(id);
        user.setReminderDays(days);
        userRepository.save(user);
        return days;
    }

    @Transactional
    public UserResponse updateRole(Long id, UpdateRoleRequest request, User actor) {
        User user = findUserOrThrow(id);
        if (user.getId().equals(actor.getId())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Não é permitido alterar o próprio papel.");
        }
        if (!List.of(Role.DEV, Role.PO, Role.MASTER).contains(request.role())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Papel inválido.");
        }
        if (user.getRole() == Role.MASTER && request.role() != Role.MASTER
                && userRepository.countByRoleIn(List.of(Role.MASTER, Role.ADMIN)) <= 1) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.CONFLICT, "O sistema precisa manter ao menos um usuário Master.");
        }
        user.setRole(request.role());
        return UserResponse.fromEntity(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = findUserOrThrow(id);
        if (user.getRole() == Role.MASTER
                && userRepository.countByRoleIn(List.of(Role.MASTER, Role.ADMIN)) <= 1) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.CONFLICT, "O sistema precisa manter ao menos um usuário Master.");
        }
        userRepository.delete(user);
    }

    private User findUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }
}
