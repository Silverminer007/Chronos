package de.kjgstbarbara.service;

import lombok.Getter;
import org.springframework.stereotype.Service;

@Service
@Getter
public class OrganisationsService {
    private final OrganisationRepository organisationRepository;

    public OrganisationsService(OrganisationRepository organisationRepository) {
        this.organisationRepository = organisationRepository;
    }
}
