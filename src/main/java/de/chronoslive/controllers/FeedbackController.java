package de.chronoslive.controllers;

import de.chronoslive.AuthorizationService;
import de.chronoslive.entitys.Date;
import de.chronoslive.entitys.Feedback;
import de.chronoslive.entitys.Person;
import de.chronoslive.repositorys.DateRepository;
import de.chronoslive.repositorys.FeedbackRepository;
import de.chronoslive.repositorys.PersonsRepository;
import de.chronoslive.utility.FrontendUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
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
    public void postFeedback(@PathVariable(name = "date") long dateID, @RequestBody String status) {
        Person principal = FrontendUtils.getAuthenticatedUser(this.personsRepository).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        try {
            Date date = dateRepository.findById(dateID).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            if (!AuthorizationService.canSee(date, principal)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
            Feedback feedback = new Feedback(principal, Feedback.Status.valueOf(status));
            feedbackRepository.save(feedback);
            date.addFeedback(feedback);
            dateRepository.save(date);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status");
        }
    }

    @GetMapping(path = "/feedback/current/{date}")
    public List<SlimFeedback> getCurrentFeedbacks(@PathVariable(name = "date") long dateID) {
        Person principal = FrontendUtils.getAuthenticatedUser(this.personsRepository).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        Date date = dateRepository.findById(dateID).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST));
        if (!AuthorizationService.canSee(date, principal)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return date.getGroup().getMembers().stream().map(date::getFeedbackFor).map(SlimFeedback::new).toList();
    }

    @GetMapping(path = "/feedback/history/{date}")
    public List<SlimFeedback> getFeedbacksHistory(@PathVariable(name = "date") long dateID) {
        Person principal = FrontendUtils.getAuthenticatedUser(this.personsRepository).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        Date date = dateRepository.findById(dateID).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST));
        if (!AuthorizationService.canSee(date, principal)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return date.getFeedbackList().stream().map(SlimFeedback::new).toList();
    }

    @GetMapping(path = "/feedback/current/{date}/{person}")
    public SlimFeedback getCurrentFeedbacks(@PathVariable(name = "date") long dateID, @PathVariable(name = "person") long personID) {
        Person principal = FrontendUtils.getAuthenticatedUser(this.personsRepository).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        Date date = dateRepository.findById(dateID).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST));
        if (!AuthorizationService.canSee(date, principal)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        Person person = personsRepository.findById(personID).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return new SlimFeedback(date.getFeedbackFor(person));
    }

    @GetMapping(path = "/feedback/history/{date}/{person}")
    public List<SlimFeedback> getFeedbacksHistory(@PathVariable(name = "date") long dateID, @PathVariable(name = "person") long personID) {
        Person principal = FrontendUtils.getAuthenticatedUser(this.personsRepository).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        Date date = dateRepository.findById(dateID).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST));
        if (!AuthorizationService.canSee(date, principal)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return date.getFeedbackList().stream().filter(f -> f.getPerson().getId() == personID).map(SlimFeedback::new).toList();
    }

    public record SlimFeedback(long person, String status, String timeStamp) {
        public SlimFeedback(Feedback feedback) {
            this(feedback.getPerson().getId(), feedback.getStatus().name(), feedback.getTimeStamp().format(DateTimeFormatter.ISO_DATE_TIME));
        }
    }
}