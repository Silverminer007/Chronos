package de.kjgstbarbara.service;

import de.kjgstbarbara.data.Organisation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganisationRepository extends JpaRepository<Organisation, String> {
}
