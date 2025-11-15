package com.skillbridge.config;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("dev")
public class FlywayDevConfig {

    private static final Logger logger = LoggerFactory.getLogger(FlywayDevConfig.class);

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            try {
                logger.info("Starting Flyway migration...");
                
                // Check if there are any failed migrations
                MigrationInfo[] allMigrations = flyway.info().all();
                boolean hasFailedMigrations = false;
                for (MigrationInfo migration : allMigrations) {
                    if (migration.getState() == MigrationState.FAILED) {
                        hasFailedMigrations = true;
                        logger.warn("Found failed migration: {} - {}", migration.getVersion(), migration.getDescription());
                    }
                }
                
                if (hasFailedMigrations) {
                    logger.warn("Found failed migrations. Attempting to repair...");
                    flyway.repair();
                    logger.info("Flyway repair completed.");
                }
                
                // Run migrations
                flyway.migrate();
                logger.info("Flyway migration completed successfully.");
                
            } catch (Exception e) {
                logger.error("Flyway migration failed: {}", e.getMessage(), e);
                throw e;
            }
        };
    }
}


