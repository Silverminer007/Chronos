package de.kjgstbarbara.service;

import de.kjgstbarbara.data.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Objects;

public interface DateRepository extends JpaRepository<Date, Long> {
    default List<Date> findByBoard(Board board) {
        return findAll().stream().filter(date -> Objects.equals(board.getId(), date.getBoard().getId())).toList();
    }
}