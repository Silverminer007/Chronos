package de.chronoslive.controllers;

import de.chronoslive.AuthorizationService;
import de.chronoslive.entitys.Group;
import de.chronoslive.entitys.Person;
import de.chronoslive.repositorys.GroupRepository;
import de.chronoslive.repositorys.OrganisationRepository;
import de.chronoslive.repositorys.PersonsRepository;
import de.chronoslive.utility.FrontendUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
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
    public List<SlimGroup> getGroups() {
        Person principal = FrontendUtils.getAuthenticatedUser(this.personsRepository).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return groupRepository.findByMembersIn(principal).stream().map(SlimGroup::new).toList();
    }

    @PostMapping(path = "/group")
    public long postGroup(@RequestBody SlimGroup group) {
        Person principal = FrontendUtils.getAuthenticatedUser(this.personsRepository).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return groupRepository.save(group.getGroup(groupRepository, organisationRepository, personsRepository, principal, true)).getId();
    }

    @PutMapping(path = "/group")
    public void putGroup(@RequestBody SlimGroup group) {
        Person principal = FrontendUtils.getAuthenticatedUser(this.personsRepository).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        groupRepository.save(group.getGroup(groupRepository, organisationRepository, personsRepository, principal, false));
    }

    @GetMapping(path = "/group/{id}")
    public SlimGroup getGroups(@PathVariable long id) {
        Person principal = FrontendUtils.getAuthenticatedUser(this.personsRepository).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return groupRepository.findById(id).filter(g -> AuthorizationService.canSee(g, principal)).map(SlimGroup::new).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST));
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
                if (!AuthorizationService.hasAdminRights(group, principal)) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN);
                }
            }
            group.setName(this.name);
            group.setColor(this.color);
            if (!group.getColor().matches("#[a-f0-9]{6}")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid color");
            }
            group.setOrganisation(organisationRepository.findById(this.organisation).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "This organisation does not exist")));
            if (!AuthorizationService.hasAdminRights(group.getOrganisation(), principal)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }
            group.setMembers(this.members.stream()
                    .map(memberId -> personsRepository.findById(memberId)
                            .filter(p ->
                                    AuthorizationService.canSee(group.getOrganisation(), p))
                            .orElseThrow(() ->
                                    new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                            "Not all members exist or are member of the organisation")))
                    .toList());
            group.setAdmins(this.admins.stream()
                    .map(memberId -> personsRepository.findById(memberId)
                            .filter(p ->
                                    AuthorizationService.canSee(group.getOrganisation(), p))
                            .orElseThrow(() ->
                                    new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                            "Not all admin exist or are member of the organisation")))
                    .toList());
            return group;
        }
    }
}