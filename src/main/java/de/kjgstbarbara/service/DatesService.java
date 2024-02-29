package de.kjgstbarbara.service;

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
