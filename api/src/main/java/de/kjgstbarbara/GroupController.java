package de.kjgstbarbara;

import de.kjgstbarbara.data.Group;
import de.kjgstbarbara.data.Person;
import de.kjgstbarbara.service.GroupRepository;
import de.kjgstbarbara.service.OrganisationRepository;
import de.kjgstbarbara.service.PersonsRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.StandardClaimNames;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
public class GroupController {
    private final GroupRepository groupRepository;
    private final OrganisationRepository organisationRepository;
    private final PersonsRepository personsRepository;

    public GroupController(GroupRepository groupRepository, OrganisationRepository organisationRepository, PersonsRepository personsRepository) {
        this.organisationRepository = organisationRepository;
        this.groupRepository = groupRepository;
        this.personsRepository = personsRepository;
    }

    @GetMapping(path = "/group")
    public List<SlimGroup> getGroups(JwtAuthenticationToken token) {
        return groupRepository.findByMembersIn(getPrincipal(token)).stream().map(SlimGroup::new).toList();
    }

    @PostMapping(path = "/group")
    public long postGroup(@RequestBody SlimGroup group, JwtAuthenticationToken token) {
        return groupRepository.save(group.getGroup(groupRepository, organisationRepository, personsRepository, getPrincipal(token), true)).getId();
    }

    @PutMapping(path = "/group")
    public void putGroup(@RequestBody SlimGroup group, JwtAuthenticationToken token) {
        groupRepository.save(group.getGroup(groupRepository, organisationRepository, personsRepository, getPrincipal(token), false));
    }

    @GetMapping(path = "/group/{id}")
    public SlimGroup getGroups(@PathVariable long id, JwtAuthenticationToken token) {
        return groupRepository.findById(id).filter(g -> Authorization.canSee(g, getPrincipal(token))).map(SlimGroup::new).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST));
    }

    private Person getPrincipal(JwtAuthenticationToken token) {
        System.out.println(token.getToken().getClaimAsString(StandardClaimNames.PREFERRED_USERNAME));
        return this.personsRepository.findByUsernameOrEmail(
                        token.getToken().getClaimAsString(StandardClaimNames.PREFERRED_USERNAME))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    public record SlimGroup(long id, String name, String color, long organisation, List<Long> members,
                            List<Long> admins) {
        public SlimGroup(Group group) {
            this(group.getId(), group.getName(), group.getColor(), group.getOrganisation().getId(),
                    group.getMembers().stream().map(Person::getId).toList(),
                    group.getAdmins().stream().map(Person::getId).toList());
        }

        public Group getGroup(GroupRepository groupRepository, OrganisationRepository organisationRepository, PersonsRepository personsRepository, Person principal, boolean allowNew) {
            Group group;
            if (allowNew) {
                group = new Group();
            } else {
                group = groupRepository.findById(this.id).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "This group does not exist"));
                if (!Authorization.hasAdminRights(group, principal)) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN);
                }
            }
            group.setName(this.name);
            group.setColor(this.color);
            if (!group.getColor().matches("#[a-f0-9]{6}")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid color");
            }
            group.setOrganisation(organisationRepository.findById(this.organisation).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "This organisation does not exist")));
            if (!Authorization.hasAdminRights(group.getOrganisation(), principal)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }
            group.setMembers(this.members.stream()
                    .map(memberId -> personsRepository.findById(memberId)
                            .filter(p ->
                                    Authorization.canSee(group.getOrganisation(), p))
                            .orElseThrow(() ->
                                    new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                            "Not all members exist or are member of the organisation")))
                    .toList());
            group.setAdmins(this.admins.stream()
                    .map(memberId -> personsRepository.findById(memberId)
                            .filter(p ->
                                    Authorization.canSee(group.getOrganisation(), p))
                            .orElseThrow(() ->
                                    new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                            "Not all admin exist or are member of the organisation")))
                    .toList());
            return group;
        }
    }
}