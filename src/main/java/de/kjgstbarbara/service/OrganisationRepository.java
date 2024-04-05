package de.kjgstbarbara.service;

import de.kjgstbarbara.data.Organisation;
import de.kjgstbarbara.data.Person;
import de.kjgstbarbara.data.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrganisationRepository extends JpaRepository<Organisation, Long> {
    default List<Organisation> hasAuthorityOn(Role.Type type, Person person) {
        return findAll().stream().filter(org -> person.hasAuthority(type, Role.Scope.ORGANISATION, org.getId())).toList();
    }
}