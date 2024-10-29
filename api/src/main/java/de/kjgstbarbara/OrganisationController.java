package de.kjgstbarbara;

import de.kjgstbarbara.data.Organisation;
import de.kjgstbarbara.data.Person;
import de.kjgstbarbara.service.OrganisationRepository;
import de.kjgstbarbara.service.PersonsRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.StandardClaimNames;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
public class OrganisationController {
    private final OrganisationRepository organisationRepository;
    private final PersonsRepository personsRepository;

    public OrganisationController(OrganisationRepository organisationRepository, PersonsRepository personsRepository) {
        this.organisationRepository = organisationRepository;
        this.personsRepository = personsRepository;
    }

    @GetMapping(path = "/organisation")
    public List<SlimOrganisation> getOrganisations(JwtAuthenticationToken token) {
        return organisationRepository.findByAdmin(getPrincipal(token)).stream().map(SlimOrganisation::new).toList();
    }

    @PostMapping(path = "/organisation")
    public long postOrganisation(@RequestBody SlimOrganisation organisation, JwtAuthenticationToken token) {
        return organisationRepository.save(organisation.getOrganisation(organisationRepository, personsRepository, getPrincipal(token), true)).getId();
    }

    @GetMapping(path = "/organisation/{id}")
    public SlimOrganisation getOrganisation(@PathVariable("id") long id, JwtAuthenticationToken token) {
        return organisationRepository.findById(id)
                .filter(o -> Authorization.canSee(o, getPrincipal(token)))
                .map(SlimOrganisation::new)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "This organisation does not exist"));
    }

    @PutMapping(path = "/organisation")
    public void putOrganisation(@RequestBody SlimOrganisation organisation, JwtAuthenticationToken token) {
        organisationRepository.save(organisation.getOrganisation(organisationRepository, personsRepository, getPrincipal(token), false));
    }


    private Person getPrincipal(JwtAuthenticationToken token) {
        System.out.println(token.getToken().getClaimAsString(StandardClaimNames.PREFERRED_USERNAME));
        return this.personsRepository.findByUsernameOrEmail(token.getToken().getClaimAsString(StandardClaimNames.PREFERRED_USERNAME)).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
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
                if (!Authorization.hasAdminRights(organisation, principal)) {
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