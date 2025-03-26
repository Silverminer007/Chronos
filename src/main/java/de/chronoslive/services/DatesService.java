package de.chronoslive.services;

import de.chronoslive.repositorys.DateRepository;
import lombok.Getter;
import org.springframework.stereotype.Service;

@Service
@Getter
public class DatesService {
    private final DateRepository dateRepository;

    public DatesService(DateRepository dateRepository) {
        this.dateRepository = dateRepository;
    }
}
