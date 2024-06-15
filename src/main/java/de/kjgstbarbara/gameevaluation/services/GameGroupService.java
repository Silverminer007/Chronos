package de.kjgstbarbara.gameevaluation.services;

import de.kjgstbarbara.gameevaluation.data.GameGroup;
import de.kjgstbarbara.gameevaluation.data.Participant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class GameGroupService {
    private final GameGroupRepository repository;

    public GameGroupService(GameGroupRepository repository) {
        this.repository = repository;
    }

    public Optional<GameGroup> get(Long id) {
        return repository.findById(id);
    }

    public GameGroup update(GameGroup entity) {
        return repository.save(entity);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public Page<GameGroup> list(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Page<GameGroup> list(Pageable pageable, Specification<GameGroup> filter) {
        return repository.findAll(filter, pageable);
    }

    public List<GameGroup> getByPerson(Participant... participantList) {
        return repository.findByParticipantsIn(List.of(participantList));
    }

    public int count() {
        return (int) repository.count();
    }
}