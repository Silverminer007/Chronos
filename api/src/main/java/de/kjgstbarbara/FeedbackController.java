package de.kjgstbarbara;

import de.kjgstbarbara.data.Date;
import de.kjgstbarbara.data.Feedback;
import de.kjgstbarbara.data.Person;
import de.kjgstbarbara.service.DateRepository;
import de.kjgstbarbara.service.FeedbackRepository;
import de.kjgstbarbara.service.PersonsRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.StandardClaimNames;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
public class FeedbackController {
    private final FeedbackRepository feedbackRepository;
    private final DateRepository dateRepository;
    private final PersonsRepository personsRepository;

    public FeedbackController(FeedbackRepository feedbackRepository, DateRepository dateRepository, PersonsRepository personsRepository) {
        this.feedbackRepository = feedbackRepository;
        this.dateRepository = dateRepository;
        this.personsRepository = personsRepository;
    }

    @PostMapping(path = "/feedback/{date}")
    public void postFeedback(@PathVariable(name = "date") long dateID, @RequestBody String status, JwtAuthenticationToken token) {
        try {
            Date date = dateRepository.findById(dateID).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            if (!Authorization.canSee(date, getPrincipal(token))) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
            Feedback feedback = new Feedback(getPrincipal(token), Feedback.Status.valueOf(status));
            feedbackRepository.save(feedback);
            date.addFeedback(feedback);
            dateRepository.save(date);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status");
        }
    }

    @GetMapping(path = "/feedback/current/{date}")
    public List<SlimFeedback> getCurrentFeedbacks(@PathVariable(name = "date") long dateID, JwtAuthenticationToken token) {
        Person principal = getPrincipal(token);
        Date date = dateRepository.findById(dateID).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST));
        if (!Authorization.canSee(date, principal)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return date.getGroup().getMembers().stream().map(date::getFeedbackFor).map(SlimFeedback::new).toList();
    }

    @GetMapping(path = "/feedback/history/{date}")
    public List<SlimFeedback> getFeedbacksHistory(@PathVariable(name = "date") long dateID, JwtAuthenticationToken token) {
        Person principal = getPrincipal(token);
        Date date = dateRepository.findById(dateID).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST));
        if (!Authorization.canSee(date, principal)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return date.getFeedbackList().stream().map(SlimFeedback::new).toList();
    }

    @GetMapping(path = "/feedback/current/{date}/{person}")
    public SlimFeedback getCurrentFeedbacks(@PathVariable(name = "date") long dateID, @PathVariable(name = "person") long personID, JwtAuthenticationToken token) {
        Person principal = getPrincipal(token);
        Date date = dateRepository.findById(dateID).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST));
        if (!Authorization.canSee(date, principal)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        Person person = personsRepository.findById(personID).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return new SlimFeedback(date.getFeedbackFor(person));
    }

    @GetMapping(path = "/feedback/history/{date}/{person}")
    public List<SlimFeedback> getFeedbacksHistory(@PathVariable(name = "date") long dateID, @PathVariable(name = "person") long personID, JwtAuthenticationToken token) {
        Person principal = getPrincipal(token);
        Date date = dateRepository.findById(dateID).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST));
        if (!Authorization.canSee(date, principal)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return date.getFeedbackList().stream().filter(f -> f.getPerson().getId() == personID).map(SlimFeedback::new).toList();
    }

    private Person getPrincipal(JwtAuthenticationToken token) {
        System.out.println(token.getToken().getClaimAsString(StandardClaimNames.PREFERRED_USERNAME));
        return this.personsRepository.findByUsername(token.getToken().getClaimAsString(StandardClaimNames.PREFERRED_USERNAME)).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    public record SlimFeedback(long person, String status, String timeStamp) {
        public SlimFeedback(Feedback feedback) {
            this(feedback.getPerson().getId(), feedback.getStatus().name(), feedback.getTimeStamp().format(DateTimeFormatter.ISO_DATE_TIME));
        }
    }
}