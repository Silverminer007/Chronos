package de.chronoslive.controllers;

import de.chronoslive.AuthorizationService;
import de.chronoslive.entitys.Date;
import de.chronoslive.entitys.Person;
import de.chronoslive.repositorys.DateRepository;
import de.chronoslive.repositorys.PersonsRepository;
import de.chronoslive.utility.FrontendUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class InformationController {
    private final DateRepository dateRepository;
    private final PersonsRepository personsRepository;

    public InformationController(DateRepository dateRepository, PersonsRepository personsRepository) {
        this.dateRepository = dateRepository;
        this.personsRepository = personsRepository;
    }

    @GetMapping(path = "/information/{date}")
    public List<Information> getInformation(@PathVariable(name = "date") long dateID) {
        Person principal = FrontendUtils.getAuthenticatedUser(this.personsRepository).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        Date date = dateRepository.findById(dateID).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid Date ID"));
        if(AuthorizationService.canSee(date, principal)) {
            return date.getInformation().stream().map(Information::new).toList();
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    @PostMapping(path = "/information/{date}")
    public Information postInformation(@PathVariable(name = "date") long dateID, @RequestBody String information) {
        Person principal = FrontendUtils.getAuthenticatedUser(this.personsRepository).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        Date.Information informationDate = new Date.Information();
        informationDate.setInformationSender(principal);
        informationDate.setInformationText(information);
        informationDate.setInformationTime(LocalDateTime.now());
        Date date = dateRepository.findById(dateID).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "This date does not exist"));
        if(AuthorizationService.canSee(date, principal)) {
            date.getInformation().add(informationDate);
            dateRepository.save(date);
            return new Information(informationDate);
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You're not allowed to post this information to this date");
        }
    }

    public record Information(long person, String timeStamp, String text) {
        public Information(Date.Information information) {
            this(information.getInformationSender().getId(), information.getInformationTime().format(DateTimeFormatter.ISO_DATE_TIME), information.getInformationText());
        }
    }
}