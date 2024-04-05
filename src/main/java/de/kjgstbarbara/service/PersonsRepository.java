package de.kjgstbarbara.service;

import de.kjgstbarbara.data.Board;
import de.kjgstbarbara.data.Organisation;
import de.kjgstbarbara.data.Person;
import de.kjgstbarbara.data.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PersonsRepository extends JpaRepository<Person, Long> {
    default Optional<Person> findByUsername(String username) {
        return findAll().stream().filter(person -> person.getUsername().equals(username)).findAny();
    }

    default Optional<Person> findByPhoneNumber(long phoneNumber) {
        return findAll().stream().filter(person -> person.getPhoneNumber() == phoneNumber).findAny();
    }

    default List<Person> hasAuthorityOn(Role.Type type, Organisation organisation) {
        return findAll().stream().filter(person -> person.hasAuthority(type, Role.Scope.ORGANISATION, organisation.getId())).toList();
    }

    default List<Person> hasAuthorityOn(Role.Type type, Board board) {
        return findAll().stream().filter(person -> person.hasAuthority(type, Role.Scope.BOARD, board.getId())).toList();
    }

    default List<Person> hasRoleOn(Role.Type type, Board board) {
        return findAll().stream().filter(person -> person.hasRole(type, Role.Scope.BOARD, board.getId())).toList();
    }
}