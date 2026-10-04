package com.KernelPanic.TaskFlow.config;

import com.KernelPanic.TaskFlow.entity.User;
import com.KernelPanic.TaskFlow.enums.Role;
import com.KernelPanic.TaskFlow.repository.UserRepository;
import com.KernelPanic.TaskFlow.security.EmailLookupHash;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/** Creates the first supervisor only when explicitly configured and the user store is empty. */
@Component
@RequiredArgsConstructor
public class MasterAccountInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MasterAccountInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${TASKFLOW_BOOTSTRAP_MASTER_EMAIL:}")
    private String bootstrapEmail;

    @Value("${TASKFLOW_BOOTSTRAP_MASTER_NAME:Master}")
    private String bootstrapName;

    @Value("${TASKFLOW_BOOTSTRAP_MASTER_PASSWORD:}")
    private String bootstrapPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (bootstrapEmail.isBlank() || bootstrapPassword.isBlank()) return;
        if (userRepository.count() != 0) {
            log.info("Master bootstrap skipped because the user store is not empty.");
            return;
        }
        if (bootstrapPassword.length() < 12 || bootstrapPassword.length() > 72) {
            throw new IllegalStateException("TASKFLOW_BOOTSTRAP_MASTER_PASSWORD must contain 12 to 72 characters.");
        }

        String email = bootstrapEmail.trim().toLowerCase(Locale.ROOT);
        User master = User.builder()
                .name(bootstrapName.trim())
                .email(email)
                .emailLookupHash(EmailLookupHash.of(email))
                .password(passwordEncoder.encode(bootstrapPassword))
                .role(Role.MASTER)
                .build();
        userRepository.saveAndFlush(master);
        log.info("Initial Master account created from deployment bootstrap settings.");
    }
}
