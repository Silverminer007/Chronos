package de.henzeob.chronos.api;

import de.henzeob.chronos.entities.Date;
import de.henzeob.chronos.exceptions.ActionNotPermittedException;
import de.henzeob.chronos.exceptions.InvalidDateException;
import de.henzeob.chronos.exceptions.InvalidDateIdException;
import de.henzeob.chronos.services.DateService;
import de.henzeob.chronos.services.ICalService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController("/caldav")
public class ICalendarController {
    private final DateService dateService;
    private final ICalService icalService;

    public ICalendarController(DateService dateService, ICalService icalService) {
        this.dateService = dateService;
        this.icalService = icalService;
    }

    @GetMapping(path = "/{dateId}.ics")
    public ResponseEntity<String> getDate(@PathVariable("dateId") String dateId) {
        try {

            Date date = dateService.readDate(dateId);
            String iCalDate = icalService.parseICalDate(date);
            return new ResponseEntity<>(iCalDate, HttpStatus.OK);

        } catch (InvalidDateIdException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (ActionNotPermittedException e) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
    }

    @PostMapping(path = "/{dateId}.ics")
    public ResponseEntity<Void> postDate(@PathVariable("dateId") String dateId, @RequestBody String iCalDate) {
        try {
            Date newDate = icalService.parseICalDate(iCalDate);
            // newDate.setId(dateId);
            dateService.createDate(newDate);
            return new ResponseEntity<>(HttpStatus.CREATED);
        } catch (InvalidDateException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } catch (ActionNotPermittedException e) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
    }

    @PutMapping(path = "/{dateId}.ics")
    public ResponseEntity<Void> putDate(@PathVariable("dateId") String dateId, @RequestBody String iCalDate) {
        try {
            Date newDate = icalService.parseICalDate(iCalDate);
            // newDate.setId(dateId);
            dateService.updateDate(newDate);
            return new ResponseEntity<>(HttpStatus.CREATED);
        } catch (InvalidDateException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } catch (ActionNotPermittedException e) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
    }

    @DeleteMapping(path = "/{dateId}.ics")
    public ResponseEntity<Void> deleteDate(@PathVariable("dateId") String dateId) {
        try {
            dateService.deleteDate(dateId);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (InvalidDateIdException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } catch (ActionNotPermittedException e) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
    }
}
