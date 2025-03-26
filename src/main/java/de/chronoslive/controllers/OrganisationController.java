package de.chronoslive.controllers;

import de.chronoslive.AuthorizationService;
import de.chronoslive.entitys.Organisation;
import de.chronoslive.entitys.Person;
import de.chronoslive.repositorys.OrganisationRepository;
import de.chronoslive.repositorys.PersonsRepository;
import de.chronoslive.utility.FrontendUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class OrganisationController {
    private final OrganisationRepository organisationRepository;
    private final PersonsRepository personsRepository;

    public OrganisationController(OrganisationRepository organisationRepository, PersonsRepository personsRepository) {
        this.organisationRepository = organisationRepository;
        this.personsRepository = personsRepository;
    }

    @GetMapping(path = "/organisation")
    public List<SlimOrganisation> getOrganisations() {
        Person principal = FrontendUtils.getAuthenticatedUser(this.personsRepository).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return organisationRepository.findByAdmin(principal).stream().map(SlimOrganisation::new).toList();
    }

    @PostMapping(path = "/organisation")
    public long postOrganisation(@RequestBody SlimOrganisation organisation) {
        Person principal = FrontendUtils.getAuthenticatedUser(this.personsRepository).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return organisationRepository.save(organisation.getOrganisation(organisationRepository, personsRepository, principal, true)).getId();
    }

    @GetMapping(path = "/organisation/{id}")
    public SlimOrganisation getOrganisation(@PathVariable("id") long id) {
        Person principal = FrontendUtils.getAuthenticatedUser(this.personsRepository).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return organisationRepository.findById(id)
                .filter(o -> AuthorizationService.canSee(o, principal))
                .map(SlimOrganisation::new)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "This organisation does not exist"));
    }

    @PutMapping(path = "/organisation")
    public void putOrganisation(@RequestBody SlimOrganisation organisation) {
        Person principal = FrontendUtils.getAuthenticatedUser(this.personsRepository).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        organisationRepository.save(organisation.getOrganisation(organisationRepository, personsRepository, principal, false));
    }

    public record SlimOrganisation(long id, String name, long admin, List<Long> members,
                                   List<Long> membershipRequests) {
        public SlimOrganisation(Organisation organisation) {
            this(organisation.getId(), organisation.getName(), organisation.getAdmin().getId(), organisation.getMembers().stream().map(Person::getId).toList(), organisation.getMembershipRequests().stream().map(Person::getId).toList());
        }

        public Organisation getOrganisation(OrganisationRepository organisationRepository, PersonsRepository personsRepository, Person principal, boolean allowNew) {
            Organisation organisation;
            if (allowNew) {
                organisation = new Organisation();
            } else {
                organisation = organisationRepository.findById(this.id).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "This organisation does not exist"));
                if (!AuthorizationService.hasAdminRights(organisation, principal)) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You're not allowed to edit this organisation");
                }
            }
            organisation.setName(this.name);
            // TODO Sollte jemand eine Organisation für jemand anderes erstellen können?
            organisation.setAdmin(personsRepository.findById(this.admin).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "This person does not exist")));
            organisation.setMembers(this.members.stream().map(memberId ->
                            personsRepository.findById(memberId).orElseThrow(() ->
                                    new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not all Members exist")))
                    .toList());
            organisation.setMembershipRequests(this.membershipRequests.stream().map(memberId ->
                            personsRepository.findById(memberId).orElseThrow(() ->
                                    new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not all Membership Requests exist")))
                    .toList());
            return organisation;
        }
    }
}