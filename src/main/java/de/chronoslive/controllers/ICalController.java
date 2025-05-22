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
        ICalendar calendar = this.icalService.getCalendarByGroup(groupID);
        String result = Biweekly.write(calendar).go();
        return ResponseEntity.status(HttpStatus.OK)
                .header("Content-Disposition", "filename=Kalender.ics")
                .body(result);
    }

    @GetMapping(path = "/organisation/{organisationId}.ics", produces = "text/calendar")
    public ResponseEntity<String> getCalendarByOrganisationID(@PathVariable("organisationId") long organisationID) {
        ICalendar calendar = this.icalService.getCalendarByOrganisation(organisationID);
        String result = Biweekly.write(calendar).go();
        return ResponseEntity.status(HttpStatus.OK)
                .header("Content-Disposition", "filename=Kalender.ics")
                .body(result);
    }

    @GetMapping(path = "/organisation/{organisationId}/public.ics", produces = "text/calendar")
    public ResponseEntity<String> getPublicCalendarByOrganisationID(@PathVariable("organisationId") long organisationID) {
        ICalendar calendar = this.icalService.getPublicCalendarByOrganisation(organisationID);
        String result = Biweekly.write(calendar).go();
        return ResponseEntity.status(HttpStatus.OK)
                .header("Content-Disposition", "filename=Kalender.ics")
                .body(result);
    }

    @GetMapping(path = "/date/{dateId}.ics", produces = "text/calendar")
    public ResponseEntity<String> getCalendarByDateID(@PathVariable("dateId") long dateId) {
        ICalendar calendar = this.icalService.getCalendarByDate(dateId);
        String result = Biweekly.write(calendar).go();
        return ResponseEntity.status(HttpStatus.OK)
                .header("Content-Disposition", "filename=Kalender.ics")
                .body(result);
    }

    @GetMapping(path = "/date/year/{year}.ics", produces = "text/calendar")
    public ResponseEntity<String> getCalendarByYear(@PathVariable("year") int year, @RequestParam(name = "token") String token) {
        ICalendar calendar = this.icalService.getCalendarByYear(year);
        String result = Biweekly.write(calendar).go();
        return ResponseEntity.status(HttpStatus.OK)
                .header("Content-Disposition", "filename=Kalender.ics")
                .body(result);
    }

    @GetMapping(path = "/ical.ics", produces = "text/calendar")
    public ResponseEntity<String> getCalendar() {
        Person principal = FrontendUtils.getAuthenticatedUser(this.personsService.getPersonsRepository()).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        ICalendar calendar = this.icalService.getCalendarByPerson(principal);
        String result = Biweekly.write(calendar).go();
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    @GetMapping(path = "/person/{person}/public.ics", produces = "text/calendar")
    public ResponseEntity<String> getPersonsPublicCalendar(@PathVariable("person") long personID) {
        Person person = this.personsService.getPersonsRepository().findById(personID).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        ICalendar calendar = this.icalService.getCalendarByPerson(person);
        String result = Biweekly.write(calendar).go();
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    @GetMapping(path = "/public.ics", produces = "text/calendar")
    public ResponseEntity<String> getPublicCalendar() {
        ICalendar calendar = this.icalService.getPublicCalendar();
        String result = Biweekly.write(calendar).go();
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }
}