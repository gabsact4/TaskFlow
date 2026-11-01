package com.KernelPanic.TaskFlow.repository;

import com.KernelPanic.TaskFlow.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.Collection;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailLookupHash(String emailLookupHash);

    Optional<User> findByWebauthnUserHandle(String webauthnUserHandle);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") Long id);

    boolean existsByEmailLookupHash(String emailLookupHash);

    List<User> findByRoleIn(Collection<com.KernelPanic.TaskFlow.enums.Role> roles);

    boolean existsByRoleIn(Collection<com.KernelPanic.TaskFlow.enums.Role> roles);

    long countByRoleIn(Collection<com.KernelPanic.TaskFlow.enums.Role> roles);
}
