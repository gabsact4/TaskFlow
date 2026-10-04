package com.KernelPanic.TaskFlow.controller;

import com.KernelPanic.TaskFlow.dto.ChangePasswordRequest;
import com.KernelPanic.TaskFlow.dto.CreateUserRequest;
import com.KernelPanic.TaskFlow.dto.PageResponse;
import com.KernelPanic.TaskFlow.dto.UpdateProfileRequest;
import com.KernelPanic.TaskFlow.dto.UpdateRoleRequest;
import com.KernelPanic.TaskFlow.dto.UserResponse;
import com.KernelPanic.TaskFlow.entity.User;
import com.KernelPanic.TaskFlow.service.UserService;
import com.KernelPanic.TaskFlow.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Gerenciamento de usuários.
 * <p>
 * As rotas {@code /me} operam sobre o próprio usuário autenticado (qualquer
 * usuário logado pode chamá-las). As rotas com {@code {id}} são administrativas
 * e exigem o papel {@code ADMIN}.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final NotificationService notificationService;

    // ------------------------------------------------------------------
    // Autoatendimento — o próprio usuário autenticado gerencia sua conta
    // ------------------------------------------------------------------

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(UserResponse.fromEntity(currentUser));
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateCurrentUser(@AuthenticationPrincipal User currentUser,
                                                           @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(currentUser.getId(), request));
    }

    @PatchMapping("/me/password")
    public ResponseEntity<Void> changeCurrentUserPassword(@AuthenticationPrincipal User currentUser,
                                                           @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(currentUser.getId(), request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteCurrentUser(@AuthenticationPrincipal User currentUser) {
        userService.deleteUser(currentUser.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me/reminder-days")
    public ResponseEntity<Integer> getReminderDays(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(user.getReminderDays());
    }

    @PutMapping("/me/reminder-days/{days}")
    public ResponseEntity<Integer> setReminderDays(@AuthenticationPrincipal User user, @PathVariable int days) {
        int savedDays = userService.setReminderDays(user.getId(), days);
        notificationService.generateDueDateNotifications();
        return ResponseEntity.ok(savedDays);
    }

    // ------------------------------------------------------------------
    // Administração — exige papel ADMIN
    // ------------------------------------------------------------------

    @PostMapping
    @PreAuthorize("hasAnyRole('MASTER', 'ADMIN')")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
                .body(userService.createUser(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('MASTER', 'ADMIN')")
    public ResponseEntity<PageResponse<UserResponse>> listUsers(
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(userService.listUsers(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MASTER', 'ADMIN')")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PutMapping("/{id}/role")
    @PreAuthorize("hasAnyRole('MASTER', 'ADMIN')")
    public ResponseEntity<UserResponse> updateUserRole(@PathVariable Long id,
                                                        @Valid @RequestBody UpdateRoleRequest request,
                                                        @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(userService.updateRole(id, request, currentUser));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('MASTER', 'ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
