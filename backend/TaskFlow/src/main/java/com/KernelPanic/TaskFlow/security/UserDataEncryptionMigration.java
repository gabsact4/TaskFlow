package com.KernelPanic.TaskFlow.security;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class UserDataEncryptionMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final EncryptedStringConverter converter = new EncryptedStringConverter();

    @Override
    public void run(ApplicationArguments args) {
        List<LegacyUser> users = jdbcTemplate.query(
                "SELECT id, name, email, email_lookup_hash FROM users "
                        + "WHERE name NOT LIKE 'enc:v1:%' OR email NOT LIKE 'enc:v1:%' "
                        + "OR email_lookup_hash IS NULL",
                (resultSet, rowNumber) -> new LegacyUser(
                        resultSet.getLong("id"),
                        resultSet.getString("name"),
                        resultSet.getString("email"),
                        resultSet.getString("email_lookup_hash")));

        for (LegacyUser user : users) {
            String email = converter.convertToEntityAttribute(user.email());
            String encryptedName = user.name().startsWith("enc:v1:")
                    ? user.name()
                    : converter.convertToDatabaseColumn(user.name());
            String encryptedEmail = user.email().startsWith("enc:v1:")
                    ? user.email()
                    : converter.convertToDatabaseColumn(email);
            String emailLookupHash = user.emailLookupHash() == null
                    ? EmailLookupHash.of(email)
                    : user.emailLookupHash();

            jdbcTemplate.update(
                    "UPDATE users SET name = ?, email = ?, email_lookup_hash = ? WHERE id = ?",
                    encryptedName, encryptedEmail, emailLookupHash, user.id());
        }
    }

    private record LegacyUser(Long id, String name, String email, String emailLookupHash) {
    }
}