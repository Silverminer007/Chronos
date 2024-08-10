package de.kjgstbarbara.chronos.service;

import lombok.Getter;
import org.springframework.stereotype.Service;

@Service
@Getter
public class PersonsService {
    private final PersonsRepository personsRepository;

    public PersonsService(PersonsRepository personsRepository) {
        this.personsRepository = personsRepository;
    }
}
