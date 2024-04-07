package de.kjgstbarbara.service;

import de.kjgstbarbara.data.Board;
import de.kjgstbarbara.data.Person;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Objects;

public interface BoardsRepository extends JpaRepository<Board, Long> {
    default List<Board> findByPerson(Person person) {
        return findAll().stream().filter(board -> board.getMembers().contains(person)).toList();
    }
}
