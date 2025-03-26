package de.chronoslive.controllers;

import de.chronoslive.AuthorizationService;
import de.chronoslive.entitys.Date;
import de.chronoslive.entitys.Group;
import de.chronoslive.entitys.Person;
import de.chronoslive.repositorys.DateRepository;
import de.chronoslive.repositorys.GroupRepository;
import de.chronoslive.repositorys.PersonsRepository;
import de.chronoslive.utility.FrontendUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/v1")
public class DateController {
    private final DateRepository dateRepository;
    private final GroupRepository groupRepository;
    private final PersonsRepository personsRepository;

    public DateController(DateRepository dateRepository, GroupRepository groupRepository, PersonsRepository personsRepository) {
        this.dateRepository = dateRepository;
        this.groupRepository = groupRepository;
        this.personsRepository = personsRepository;
    }

    @GetMapping(path = "/date")
    public List<SlimDate> getDate(
            @RequestParam(required = false) List<Long> groups,
            @RequestParam(required = false) List<Long> organisations,
            @RequestParam(required = false) String after,
            @RequestParam(required = false) String before) {
        Person principal = FrontendUtils.getAuthenticatedUser(this.personsRepository).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        LocalDateTime fromDate = after == null ? null : LocalDateTime.parse(after, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        LocalDateTime untilDate = before == null ? null : LocalDateTime.parse(before, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        Stream<Date> baseList =
                fromDate == null
                        ?
                        untilDate == null ?
                                dateRepository.findByGroupMembersIn(List.of(principal)).stream() :
                                dateRepository.findByStartBetweenAndGroupMembersIn(untilDate.minus(1, ChronoUnit.CENTURIES), untilDate,
                                        List.of(principal)).stream()
                        :
                        untilDate == null ?
                                dateRepository.findByStartBetweenAndGroupMembersIn(fromDate, fromDate.plus(1, ChronoUnit.CENTURIES),
                                        List.of(principal)).stream() :
                                dateRepository.findByStartBetweenAndGroupMembersIn(fromDate, untilDate,
                                        List.of(principal)).stream();
        if (groups != null) {
            baseList = baseList.filter(d -> groups.contains(d.getGroup().getId()));
        }
        if (organisations != null) {
            baseList = baseList.filter(d -> organisations.contains(d.getGroup().getOrganisation().getId()));
        }
        return baseList.map(SlimDate::new).toList();
    }

    @PostMapping(path = "/date")
    public long postDate(@RequestBody SlimDate date) {
        Person principal = FrontendUtils.getAuthenticatedUser(this.personsRepository).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        Date d = dateRepository.save(date.getDate(dateRepository, groupRepository, principal, true));
        return d.getId();
    }

    @GetMapping(path = "/date/{id}")
    public SlimDate getDate(@PathVariable Long id) {
        Person principal = FrontendUtils.getAuthenticatedUser(this.personsRepository).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        Optional<Date> d = this.dateRepository.findById(id).filter(date -> AuthorizationService.canSee(date, principal));
        if (d.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return new SlimDate(d.get());
    }

    @PutMapping(path = "/date")
    public void putDate(@RequestBody SlimDate date) {
        Person principal = FrontendUtils.getAuthenticatedUser(this.personsRepository).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        dateRepository.save(date.getDate(dateRepository, groupRepository, principal, false));
        throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }

    public record SlimDate(long id, String title, String start, String end, String venue, String notes, long group,
                           long linkedTo, boolean cancelled, boolean pollRunning, String pollScheduled) {
        public SlimDate(Date date) {
            this(date.getId(), date.getTitle(), date.getStart().format(DateTimeFormatter.ISO_DATE_TIME), date.getEnd().format(DateTimeFormatter.ISO_DATE_TIME), date.getVenue(), date.getNotes(), date.getGroup().getId(), date.getLinkedTo(), date.getDateCancelled() == null, date.isPollRunning(), Optional.ofNullable(date.getPollScheduledFor()).map(poll -> poll.format(DateTimeFormatter.ISO_DATE)).orElse(null));
        }

        public Date getDate(DateRepository dateRepository, GroupRepository groupRepository, Person principal, boolean allowNew) {
            Date date;
            if (allowNew) {
                date = new Date();
            } else {
                date = dateRepository.findById(this.id).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Date ID"));
                if (!AuthorizationService.hasAdminRights(date, principal)) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN);
                }
            }
            date.setTitle(this.title);
            date.setStart(LocalDateTime.parse(start, DateTimeFormatter.ISO_DATE_TIME));// TODO Validate
            date.setEnd(LocalDateTime.parse(end, DateTimeFormatter.ISO_DATE_TIME));
            date.setVenue(this.venue);
            date.setNotes(this.notes);
            Group group = groupRepository.findById(this.group).orElseThrow(() ->
                    new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Group ID"));
            date.setGroup(group);
            if (!AuthorizationService.hasAdminRights(date, principal)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }
            if (date.getLinkedTo() != this.linkedTo) {
                if (this.linkedTo >= 0) {
                    Date linkedToDate = dateRepository.findById(this.linkedTo).orElseThrow(() ->
                            new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid linked date"));
                    if (linkedToDate.getGroup() != date.getGroup()) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Linked dates must be part of the same group");
                    }
                }
                date.setLinkedTo(this.linkedTo);
            }
            date.setDateCancelled(this.cancelled
                    ? date.getDateCancelled() == null ? LocalDate.now() : date.getDateCancelled()
                    : null);
            date.setPollRunning(this.pollRunning);
            date.setPollScheduledFor(LocalDate.parse(this.pollScheduled, DateTimeFormatter.ISO_DATE));
            return date;
        }
    }
}