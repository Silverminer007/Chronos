package de.chronoslive.controllers;

import biweekly.Biweekly;
import biweekly.ICalendar;
import de.chronoslive.entitys.Person;
import de.chronoslive.services.ICalService;
import de.chronoslive.services.PersonsService;
import de.chronoslive.utility.FrontendUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.TimeZone;

@RestController
@RequestMapping(path = "/api/v1/ical")
public class ICalController {
    private final ICalService icalService;
    private final PersonsService personsService;

    public ICalController(ICalService icalService, PersonsService personsService) {
        this.icalService = icalService;
        this.personsService = personsService;
    }

    @GetMapping(path = "/group/{groupID}.ics", produces = "text/calendar")
    public ResponseEntity<String> getCalendarByGroupID(@PathVariable("groupID") long groupID) {
        return writeCalendar(this.icalService.getCalendarByGroup(groupID));
    }

    @GetMapping(path = "/organisation/{organisationId}.ics", produces = "text/calendar")
    public ResponseEntity<String> getCalendarByOrganisationID(@PathVariable("organisationId") long organisationID) {
        return writeCalendar(this.icalService.getCalendarByOrganisation(organisationID));
    }

    @GetMapping(path = "/organisation/{organisationId}/public.ics", produces = "text/calendar")
    public ResponseEntity<String> getPublicCalendarByOrganisationID(@PathVariable("organisationId") long organisationID) {
        return writeCalendar(this.icalService.getPublicCalendarByOrganisation(organisationID));
    }

    @GetMapping(path = "/date/{dateId}.ics", produces = "text/calendar")
    public ResponseEntity<String> getCalendarByDateID(@PathVariable("dateId") long dateId) {
        return writeCalendar(this.icalService.getCalendarByDate(dateId));
    }

    @GetMapping(path = "/date/year/{year}.ics", produces = "text/calendar")
    public ResponseEntity<String> getCalendarByYear(@PathVariable("year") int year, @RequestParam(name = "token") String token) {
        return writeCalendar(this.icalService.getCalendarByYear(year));
    }

    @GetMapping(path = "/ical.ics", produces = "text/calendar")
    public ResponseEntity<String> getCalendar() {
        Person principal = FrontendUtils.getAuthenticatedUser(this.personsService.getPersonsRepository()).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return writeCalendar(this.icalService.getCalendarByPerson(principal));
    }

    @GetMapping(path = "/person/{person}/public.ics", produces = "text/calendar")
    public ResponseEntity<String> getPersonsPublicCalendar(@PathVariable("person") long personID) {
        Person person = this.personsService.getPersonsRepository().findById(personID).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return writeCalendar(this.icalService.getCalendarByPerson(person));
    }

    @GetMapping(path = "/public.ics", produces = "text/calendar")
    public ResponseEntity<String> getPublicCalendar() {
        return writeCalendar(this.icalService.getPublicCalendar());
    }

    private ResponseEntity<String> writeCalendar(ICalendar calendar) {
        String result = Biweekly.write(calendar).tz(TimeZone.getTimeZone("Europe/Berlin"), false).go();
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }
}