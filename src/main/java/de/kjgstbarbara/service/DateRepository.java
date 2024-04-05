package de.kjgstbarbara.service;

import de.kjgstbarbara.data.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DateRepository extends JpaRepository<Date, Long> {
    default List<Date> hasAuthorityOn(Role.Type type, Person person) {
        return findAll().stream().filter(date -> person.hasAuthority(type, Role.Scope.BOARD, date.getBoard().getId())).toList();
    }

    default List<Date> findByOrganisation(Organisation organisation) {
        return findAll().stream().filter(date -> date.getBoard().getOrganisation().equals(organisation)).toList();
    }
}
