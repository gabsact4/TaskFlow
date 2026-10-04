package com.KernelPanic.TaskFlow.repository;

import com.KernelPanic.TaskFlow.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.Collection;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailLookupHash(String emailLookupHash);

    Optional<User> findByWebauthnUserHandle(String webauthnUserHandle);

    boolean existsByEmailLookupHash(String emailLookupHash);

    List<User> findByRoleIn(Collection<com.KernelPanic.TaskFlow.enums.Role> roles);

    boolean existsByRoleIn(Collection<com.KernelPanic.TaskFlow.enums.Role> roles);

    long countByRoleIn(Collection<com.KernelPanic.TaskFlow.enums.Role> roles);
}
