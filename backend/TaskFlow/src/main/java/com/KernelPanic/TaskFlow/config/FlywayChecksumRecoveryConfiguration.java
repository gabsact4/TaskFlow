package com.KernelPanic.TaskFlow.config;

import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Repairs only the exact successful V7/V8 checksum drift reported by existing TaskFlow databases. */
@Configuration
public class FlywayChecksumRecoveryConfiguration {

    private static final Logger log = LoggerFactory.getLogger(FlywayChecksumRecoveryConfiguration.class);
    private static final Map<String, ChecksumPair> KNOWN_CHECKSUM_CHANGES = Map.of(
            "7", new ChecksumPair(2030684103, -180579027),
            "8", new ChecksumPair(1129830450, -1047912790));

    @Bean
    FlywayMigrationStrategy flywayMigrationStrategy(
            @Value("${taskflow.flyway.repair-known-checksums:false}") boolean repairKnownChecksums) {
        return flyway -> {
            List<MigrationInfo> migrations = List.of(flyway.info().all());
            if (migrations.stream().anyMatch(info -> info.getState() == MigrationState.FAILED)) {
                throw new IllegalStateException("Flyway possui migrations com falha; reparo automático cancelado.");
            }

            List<MigrationInfo> changed = migrations.stream()
                    .filter(info -> info.getAppliedChecksum() != null
                            && !Objects.equals(info.getAppliedChecksum(), info.getResolvedChecksum()))
                    .toList();
            if (!changed.isEmpty()) {
                boolean exactKnownDrift = changed.size() == KNOWN_CHECKSUM_CHANGES.size()
                        && changed.stream().allMatch(FlywayChecksumRecoveryConfiguration::isKnownSuccessfulChange);
                if (!repairKnownChecksums || !exactKnownDrift) {
                    throw new IllegalStateException("Flyway encontrou divergência de checksum não autorizada; "
                            + "nenhum reparo automático foi executado.");
                }
                log.warn("Reparando somente metadados Flyway das migrations V7/V8 conhecidas; "
                        + "nenhuma tabela ou linha de negócio será removida.");
                flyway.repair();
            }
            flyway.migrate();
        };
    }

    static boolean isKnownSuccessfulChange(MigrationInfo info) {
        if (info.getVersion() == null || info.getState() != MigrationState.SUCCESS) return false;
        ChecksumPair expected = KNOWN_CHECKSUM_CHANGES.get(info.getVersion().getVersion());
        return expected != null
                && Objects.equals(info.getAppliedChecksum(), expected.applied())
                && Objects.equals(info.getResolvedChecksum(), expected.resolved());
    }

    private record ChecksumPair(Integer applied, Integer resolved) {
    }
}
