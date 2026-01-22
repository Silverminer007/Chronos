package de.chronoslive.controllers;

import de.chronoslive.migration.MigrationService;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/migration")
@RolesAllowed("ADMIN_API")
public class MigrationController {
    private final MigrationService migrationService;

    public MigrationController(MigrationService migrationService) {
        this.migrationService = migrationService;
    }

    @PostMapping("/start")
    public void startMigration() {
        this.migrationService.startMigration();
    }
}
