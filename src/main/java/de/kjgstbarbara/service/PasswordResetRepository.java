package de.kjgstbarbara.service;

import de.kjgstbarbara.data.PasswordReset;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetRepository extends JpaRepository<PasswordReset, String> {
}
