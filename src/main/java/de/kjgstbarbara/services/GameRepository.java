package de.kjgstbarbara.services;

import de.kjgstbarbara.data.Game;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface GameRepository extends JpaRepository<Game, Long>, JpaSpecificationExecutor<Game> {
}