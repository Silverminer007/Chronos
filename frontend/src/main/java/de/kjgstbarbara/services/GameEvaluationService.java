package de.kjgstbarbara.services;

import de.kjgstbarbara.data.GameEvaluation;
import de.kjgstbarbara.data.Person;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class GameEvaluationService {
    private final GameEvaluationRepository repository;

    public GameEvaluationService(GameEvaluationRepository repository) {
        this.repository = repository;
    }

    public Optional<GameEvaluation> get(Long id) {
        return repository.findById(id);
    }

    public GameEvaluation update(GameEvaluation entity) {
        return repository.save(entity);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public Page<GameEvaluation> listForPerson(Pageable pageable, Person person) {
        return repository.findByGroupMembersIn(pageable, List.of(person));
    }

    public Page<GameEvaluation> list(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Page<GameEvaluation> list(Pageable pageable, Specification<GameEvaluation> filter) {
        return repository.findAll(filter, pageable);
    }

    public int count() {
        return (int) repository.count();
    }
}