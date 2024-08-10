package de.kjgstbarbara.chronos.services;

import de.kjgstbarbara.chronos.data.Participant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ParticipantService {
    private final ParticipantRepository repository;

    public ParticipantService(ParticipantRepository repository) {
        this.repository = repository;
    }

    public Optional<Participant> get(Long id) {
        return repository.findById(id);
    }

    public Participant update(Participant entity) {
        return repository.save(entity);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public Page<Participant> list(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Page<Participant> list(Pageable pageable, Specification<Participant> filter) {
        return repository.findAll(filter, pageable);
    }

    public int count() {
        return (int) repository.count();
    }
}