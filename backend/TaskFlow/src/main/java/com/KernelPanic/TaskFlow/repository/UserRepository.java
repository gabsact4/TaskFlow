package com.KernelPanic.TaskFlow.repository;

import com.KernelPanic.TaskFlow.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailLookupHash(String emailLookupHash);

    Optional<User> findByWebauthnUserHandle(String webauthnUserHandle);

    boolean existsByEmailLookupHash(String emailLookupHash);
}
