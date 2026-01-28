package de.chronoslive.migration;

import de.chronoslive.entitys.*;
import de.chronoslive.migration.api.AppointmentClient;
import de.chronoslive.migration.api.FriendshipClient;
import de.chronoslive.migration.api.GroupClient;
import de.chronoslive.migration.api.UserClient;
import de.chronoslive.migration.dto.*;
import de.chronoslive.repositorys.DateRepository;
import de.chronoslive.repositorys.GroupRepository;
import de.chronoslive.repositorys.OrganisationRepository;
import de.chronoslive.repositorys.PersonsRepository;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MigrationService {
    private static final Logger LOG = LoggerFactory.getLogger(MigrationService.class);
    private final KeycloakAdminService keycloakAdminService;

    private final PersonsRepository personsRepository;
    private final GroupRepository groupRepository;
    private final DateRepository dateRepository;
    private final OrganisationRepository organisationRepository;

    private final UserClient userClient;
    private final FriendshipClient friendshipClient;
    private final GroupClient groupClient;
    private final AppointmentClient appointmentClient;

    public MigrationService(KeycloakAdminService keycloakAdminService, PersonsRepository personsRepository, GroupRepository groupRepository, DateRepository dateRepository, OrganisationRepository organisationRepository, UserClient userClient, FriendshipClient friendshipClient, GroupClient groupClient, AppointmentClient appointmentClient) {
        this.keycloakAdminService = keycloakAdminService;
        this.personsRepository = personsRepository;
        this.groupRepository = groupRepository;
        this.dateRepository = dateRepository;
        this.organisationRepository = organisationRepository;
        this.userClient = userClient;
        this.friendshipClient = friendshipClient;
        this.groupClient = groupClient;
        this.appointmentClient = appointmentClient;
    }

    public void startMigration() {
        Map<Long, Long> personUserIdMap = new HashMap<>();
        Map<Long, Long> groupIdMap = new HashMap<>();

        List<UserRepresentation> keycloakUserList = this.keycloakAdminService.listUsers();

        // User Migration
        List<Person> personList = this.personsRepository.findAll();

        for (Person person : personList) {
            CreateUserDto createUserDto = new CreateUserDto();
            createUserDto.setFirstName(person.getFirstName());
            createUserDto.setLastName(person.getLastName());
            createUserDto.setEmail(person.getEMailAddress());

            for (UserRepresentation keycloakUser : keycloakUserList) {
                if (keycloakUser.getEmail().equals(person.getEMailAddress())
                        || keycloakUser.getUsername().equals(person.getUsername())) {
                    createUserDto.setOidcId(keycloakUser.getId());
                }
            }
            if (createUserDto.getOidcId() == null) {
                continue;
            }

            UserDto createdUser = this.userClient.createUser(createUserDto);

            personUserIdMap.put(person.getId(), createdUser.id());
        }

        List<Organisation> organizationList = this.organisationRepository.findAll();

        for (Organisation organisation : organizationList) {
            List<Person> members = organisation.getMembers();

            FriendGroup friendGroup = new FriendGroup();
            friendGroup.setUserIds(members.stream().map(Person::getId).map(personUserIdMap::get).toList());

            LOG.info("Creating Friend Group {}", friendGroup);
            this.friendshipClient.befriend(friendGroup);
        }

        List<Group> groupList = this.groupRepository.findAll();

        for (Group group : groupList) {
            List<Person> members = group.getMembers();
            Person owner = group.getOrganisation().getAdmin();

            CreateGroupDto createGroupDto = new CreateGroupDto();
            createGroupDto.setOwnerId(owner.getId());
            createGroupDto.setGroupName(group.getName());
            Long createdGroupId = this.groupClient.createGroup(createGroupDto);
            groupIdMap.put(group.getId(), createdGroupId);

            for (Person member : members) {
                this.groupClient.addGroupMember(createdGroupId, personUserIdMap.get(member.getId()));
            }
        }

        List<Date> dateList = this.dateRepository.findAll();

        for (Date date : dateList) {
            if (date.getStart().isAfter(date.getEnd())) {
                continue;
            }

            AppointmentDto appointmentDto = new AppointmentDto();
            appointmentDto.setName(date.getTitle());
            appointmentDto.setDescription(date.getNotes());
            appointmentDto.setVenue(date.getVenue());
            appointmentDto.setStart(localDateToISOString(date.getStart()));
            appointmentDto.setEnd(localDateToISOString(date.getEnd()));
            appointmentDto.setMinimal_attendees(date.getLinkedTo() == -1 ? 6 : 3);

            LOG.info("Creating appointment {} from {} until {}", appointmentDto.getName(), appointmentDto.getStart(), appointmentDto.getEnd());
            AppointmentDto createdAppointmentDto = this.appointmentClient.createAppointment(appointmentDto);

            AddGroupParticipantDto addGroupParticipantDto = new AddGroupParticipantDto();
            addGroupParticipantDto.setGroup_id(groupIdMap.get(date.getGroup().getId()));
            addGroupParticipantDto.setUser_role("ATTENDANT");

            this.appointmentClient.addGroupParticipant(createdAppointmentDto.getId(), addGroupParticipantDto);

            for (Person participant : date.getGroup().getMembers()) {
                Feedback.Status status = date.getStatusFor(participant);

                ChangeParticipationStatusDto changeParticipationStatusDto = new ChangeParticipationStatusDto();
                changeParticipationStatusDto.setUserId(personUserIdMap.get(participant.getId()));
                changeParticipationStatusDto.setAppointmentId(createdAppointmentDto.getId());
                changeParticipationStatusDto.setParticipationStatus(feedbackStatusToParticipationStatus(status));
                if (status.equals(Feedback.Status.NONE)) {
                    continue;
                }

                this.appointmentClient.changeParticipationStatus(changeParticipationStatusDto);
            }

            List<Date.Information> informationList = date.getInformation();

            for (Date.Information information : informationList) {
                MessageDto messageDto = new MessageDto();
                messageDto.setBody(information.getInformationText());
                messageDto.setSender_id(information.getInformationSender().getId());
                messageDto.setAppointment_id(createdAppointmentDto.getId());
                messageDto.setTimestamp(localDateToISOString(information.getInformationTime()));

                this.appointmentClient.sendMessage(messageDto);
            }
        }
    }

    private String localDateToISOString(LocalDateTime localDateTime) {
        return localDateTime.atZone(ZoneId.of("Europe/Berlin")).withZoneSameInstant(ZoneId.of("UTC")).toInstant().toString();
    }

    private String feedbackStatusToParticipationStatus(Feedback.Status status) {
        if (Feedback.Status.CANCELLED.equals(status)) {
            return "REJECTED";
        }
        if (Feedback.Status.COMMITTED.equals(status)) {
            return "APPROVED";
        }
        return "PENDING";
    }
}
