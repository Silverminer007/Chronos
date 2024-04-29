package de.kjgstbarbara.service;

import de.kjgstbarbara.data.Config;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfigRepository extends JpaRepository<Config, String> {
}
