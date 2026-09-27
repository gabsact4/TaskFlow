package com.KernelPanic.TaskFlow.service;

import com.KernelPanic.TaskFlow.dto.ChangePasswordRequest;
import com.KernelPanic.TaskFlow.dto.PageResponse;
import com.KernelPanic.TaskFlow.dto.UpdateProfileRequest;
import com.KernelPanic.TaskFlow.dto.UpdateRoleRequest;
import com.KernelPanic.TaskFlow.dto.UserResponse;
import com.KernelPanic.TaskFlow.entity.User;
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

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        return UserResponse.fromEntity(findUserOrThrow(id));
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
    public UserResponse updateRole(Long id, UpdateRoleRequest request) {
        User user = findUserOrThrow(id);
        user.setRole(request.role());
        return UserResponse.fromEntity(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException(id);
        }
        userRepository.deleteById(id);
    }

    private User findUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }
}
