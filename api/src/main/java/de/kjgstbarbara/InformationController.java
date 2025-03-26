package de.kjgstbarbara;

import de.kjgstbarbara.data.Date;
import de.kjgstbarbara.data.Person;
import de.kjgstbarbara.service.DateRepository;
import de.kjgstbarbara.service.PersonsRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.StandardClaimNames;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
public class InformationController {
    private final DateRepository dateRepository;
    private final PersonsRepository personsRepository;

    public InformationController(DateRepository dateRepository, PersonsRepository personsRepository) {
        this.dateRepository = dateRepository;
        this.personsRepository = personsRepository;
    }

    @GetMapping(path = "/information/{date}")
    public List<Information> getInformation(@PathVariable(name = "date") long dateID, JwtAuthenticationToken token) {
        Date date = dateRepository.findById(dateID).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid Date ID"));
        if(AuthorizationService.canSee(date, getPrincipal(token))) {
            return date.getInformation().stream().map(Information::new).toList();
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    @PostMapping(path = "/information/{date}")
    public Information postInformation(@PathVariable(name = "date") long dateID, @RequestBody String information, JwtAuthenticationToken token) {
        Date.Information informationDate = new Date.Information();
        informationDate.setInformationSender(this.getPrincipal(token));
        informationDate.setInformationText(information);
        informationDate.setInformationTime(LocalDateTime.now());
        Date date = dateRepository.findById(dateID).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "This date does not exist"));
        if(AuthorizationService.canSee(date, getPrincipal(token))) {
            date.getInformation().add(informationDate);
            dateRepository.save(date);
            return new Information(informationDate);
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You're not allowed to post this information to this date");
        }
    }

    private Person getPrincipal(JwtAuthenticationToken token) {
        System.out.println(token.getToken().getClaimAsString(StandardClaimNames.PREFERRED_USERNAME));
        return this.personsRepository.findByUsername(token.getToken().getClaimAsString(StandardClaimNames.PREFERRED_USERNAME)).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    public record Information(long person, String timeStamp, String text) {
        public Information(Date.Information information) {
            this(information.getInformationSender().getId(), information.getInformationTime().format(DateTimeFormatter.ISO_DATE_TIME), information.getInformationText());
        }
    }
}