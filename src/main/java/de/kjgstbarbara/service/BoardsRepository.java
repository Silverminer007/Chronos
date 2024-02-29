package de.kjgstbarbara.service;

import de.kjgstbarbara.data.Board;
import de.kjgstbarbara.data.Organisation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Objects;

public interface BoardsRepository extends JpaRepository<Board, Long> {// TODO Move all tasks to service to have better control over accesses
    default List<Board> findByOrg(Organisation organisation) {
        return organisation == null ? List.of() : findByOrg(organisation.getId());
    }

    default List<Board> findByOrg(String organisation) {
        return findAll().stream().filter(board -> Objects.equals(organisation, board.getOrganisation().getId())).toList();
    }
}
