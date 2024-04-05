package de.kjgstbarbara.service;

import de.kjgstbarbara.data.Board;
import de.kjgstbarbara.data.Organisation;
import de.kjgstbarbara.data.Person;
import de.kjgstbarbara.data.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Objects;

public interface BoardsRepository extends JpaRepository<Board, Long> {// TODO Move all tasks to service to have better control over accesses
    default List<Board> findByOrg(Organisation organisation) {
        return organisation == null ? List.of() : findByOrg(organisation.getId());
    }

    default List<Board> findByOrg(long organisation) {
        return findAll().stream().filter(board -> Objects.equals(organisation, board.getOrganisation().getId())).toList();
    }

    default List<Board> hasAuthorityOn(Role.Type type, Person person) {
        return findAll().stream().filter(board -> person.hasAuthority(type, Role.Scope.BOARD, board.getId())).toList();
    }
}
