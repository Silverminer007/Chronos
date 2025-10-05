package de.chronoslive.controllers;

import de.chronoslive.dtos.*;
import de.chronoslive.entitys.*;
import de.chronoslive.mapper.*;
import de.chronoslive.services.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@RestController
@RequestMapping("/api/v1")
public class AdminController {
    private final DatesService datesService;
    private final GroupService groupService;
    private final OrganisationService organisationService;
    private final PersonsService personsService;
    private final FeedbackService feedbackService;

    private final DateMapper dateMapper;
    private final AttendanceMapper attendanceMapper;
    private final UserMapper userMapper;
    private final GroupMapper groupMapper;
    private final OrganisationMapper organisationMapper;
    private final MessageMapper messageMapper;

    public AdminController(DatesService datesService, GroupService groupService,
                           OrganisationService organisationService, PersonsService personsService, FeedbackService feedbackService,
                           DateMapper dateMapper, AttendanceMapper attendanceMapper, UserMapper userMapper,
                           GroupMapper groupMapper, OrganisationMapper organisationMapper, MessageMapper messageMapper) {
        this.datesService = datesService;
        this.groupService = groupService;
        this.organisationService = organisationService;
        this.personsService = personsService;
        this.feedbackService = feedbackService;
        this.dateMapper = dateMapper;
        this.attendanceMapper = attendanceMapper;
        this.userMapper = userMapper;
        this.groupMapper = groupMapper;
        this.organisationMapper = organisationMapper;
        this.messageMapper = messageMapper;
    }

    @GetMapping("/admin/export")
    public ResponseEntity<ExportObjectDto> export() {
        List<DateDto> dates = this.datesService.getDateRepository().findAll().stream().map(dateMapper::toDto).toList();
        List<AttendanceDto> attendances = this.datesService.getDateRepository().findAll().stream()
                .flatMap(d -> d.getFeedbackList().stream().map(attendanceMapper::toDto)
                        .map(a ->
                                new AttendanceDto(a.id(), a.user_id(), d.getId(), a.status(), a.lastChanged())
                        )
                ).toList();
        List<UserDto> users = this.personsService.getPersonsRepository().findAll().stream().map(userMapper::toDto).toList();
        List<GroupDto> groups = this.groupService.getGroupRepository().findAll().stream().map(groupMapper::toDto).toList();

        List<GroupUserDto> groupUserDtoList = new ArrayList<>();
        this.groupService.getGroupRepository().findAll().forEach(group -> {
            group.getMembers().forEach(member ->
                    groupUserDtoList.add(new GroupUserDto(group.getId(), member.getId(), "MEMBER")));
            group.getAdmins().forEach(admin ->
                    groupUserDtoList.add(new GroupUserDto(group.getId(), admin.getId(), "ADMIN")));
        });

        List<OrganisationDto> organisations = this.organisationService.getOrganisationRepository().findAll().stream()
                .map(organisationMapper::toDto).toList();

        List<OrganisationUserDto> organisationUserDtoList = new ArrayList<>();
        this.organisationService.getOrganisationRepository().findAll().forEach(org -> {
            organisationUserDtoList.add(new OrganisationUserDto(org.getId(), org.getAdmin().getId(), "ADMIN"));
            org.getMembers().forEach(user ->
                    organisationUserDtoList.add(new OrganisationUserDto(org.getId(), user.getId(), "MEMBER")));
            org.getMembershipRequests().forEach(user ->
                    organisationUserDtoList.add(new OrganisationUserDto(org.getId(), user.getId(), "REQUEST")));
        });

        List<MessageDto> messages = new ArrayList<>();
        this.datesService.getDateRepository().findAll().forEach(date ->
                date.getInformation().stream().map(messageMapper::toDto).forEach(messages::add));

        ExportObjectDto exportObject = new ExportObjectDto(
                dates,
                users,
                groups,
                groupUserDtoList,
                organisations,
                organisationUserDtoList,
                messages,
                attendances
        );
        return ResponseEntity.ok(exportObject);
    }
}