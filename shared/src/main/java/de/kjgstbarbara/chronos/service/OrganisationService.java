package de.kjgstbarbara.chronos.service;

import lombok.Getter;
import org.springframework.stereotype.Service;

@Service
@Getter
public class OrganisationService {
    private final OrganisationRepository organisationRepository;

    public OrganisationService(OrganisationRepository organisationRepository) {
        this.organisationRepository = organisationRepository;
    }
}
