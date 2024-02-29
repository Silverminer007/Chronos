package de.kjgstbarbara.service;

import de.kjgstbarbara.data.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, String> {
}
