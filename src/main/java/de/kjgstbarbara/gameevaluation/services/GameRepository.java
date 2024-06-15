package de.kjgstbarbara.gameevaluation.services;

import de.kjgstbarbara.gameevaluation.data.Game;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface GameRepository extends JpaRepository<Game, Long>, JpaSpecificationExecutor<Game> {
}