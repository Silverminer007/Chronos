package de.kjgstbarbara.gameevaluation.services;

import de.kjgstbarbara.gameevaluation.data.Game;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class GameService {
    private final GameRepository repository;

    public GameService(GameRepository repository) {
        this.repository = repository;
    }

    public Optional<Game> get(Long id) {
        return repository.findById(id);
    }

    public Game update(Game entity) {
        return repository.save(entity);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public Page<Game> list(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Page<Game> list(Pageable pageable, Specification<Game> filter) {
        return repository.findAll(filter, pageable);
    }

    public int count() {
        return (int) repository.count();
    }
}