package de.kjgstbarbara;

import de.kjgstbarbara.data.Person;
import de.kjgstbarbara.messaging.Platform;
import de.kjgstbarbara.service.PersonsRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.StandardClaimNames;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.text.MessageFormat;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;

@RestController
public class PersonController {
    private final PersonsRepository personsRepository;

    public PersonController(PersonsRepository personsRepository) {
        this.personsRepository = personsRepository;
    }

    @GetMapping(path = "/person/{id}/profile")
    public String getProfileImage(@PathVariable long id) {
        return personsRepository.findById(id).map(Person::getProfileImage).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PutMapping(path = "/person/{id}/profile")
    public void updateProfileImage(@PathVariable long id, @RequestBody String profileImage, JwtAuthenticationToken token) {
        Person person = personsRepository.findById(id).filter(p -> p.equals(getPrincipal(token)))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        person.setProfileImage(profileImage);
        personsRepository.save(person);
    }

    @GetMapping(path = "/person")
    public SlimPerson getPerson(JwtAuthenticationToken token) {
        return new SlimPerson(getPrincipal(token));
    }

    @GetMapping(path = "/person/{id}")
    public SlimPerson getPerson(@PathVariable long id) {
        return new SlimPerson(personsRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
    }

    @PutMapping(path = "/person")
    public void putPerson(@RequestBody SlimPerson person, JwtAuthenticationToken token) {
        this.personsRepository.save(person.getPerson(personsRepository, getPrincipal(token), false));
    }

    private Person getPrincipal(JwtAuthenticationToken token) {
        System.out.println(token.getToken().getClaimAsString(StandardClaimNames.PREFERRED_USERNAME));
        return this.personsRepository.findByUsernameOrEmail(token.getToken().getClaimAsString(StandardClaimNames.PREFERRED_USERNAME)).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    public record SlimPerson(long id, String firstName, String lastName, String username, boolean darkMode, String locale, String timeZone, String calendarLayout, String platform, List<SlimNotification> notifications) {
        public SlimPerson(Person person) {
            this(person.getId(), person.getFirstName(), person.getLastName(), person.getUsername(), person.isDarkMode(), mapLocale(person.getUserLocale()), person.getTimezone().toString(), person.getCalendarLayout().name(), person.getPrefferedPlatform().name(), person.getNotifications().stream().map(SlimNotification::new).toList());
        }

        private static String mapLocale(Locale locale) {
            return MessageFormat.format("{0}-{1}",
                    locale.getLanguage(),
                    locale.getCountry());
        }

        public Person getPerson(PersonsRepository personsRepository, Person principal, boolean allowNew) {
            Person person;
            if(allowNew) {
                person = new Person();
            } else {
                person = personsRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
                if(!person.equals(principal)) {
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
                }
            }
            person.setFirstName(this.firstName);
            person.setLastName(this.lastName);
            person.setDarkMode(this.darkMode);
            person.setUserLocale(Locale.forLanguageTag(this.locale));
            person.setTimezone(ZoneId.of(this.timeZone));
            try {
                person.setCalendarLayout(Person.CalendarLayout.valueOf(this.calendarLayout));
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Calendar Layout");
            }
            try {
                person.setPrefferedPlatform(Platform.valueOf(this.platform));
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Platform");
            }
            person.setNotifications(this.notifications.stream().map(SlimNotification::getNotification).toList());
            return person;
        }
    }

    public record SlimNotification(String platform, int hoursBefore) {
        public SlimNotification(Person.Notification notification) {
            this(notification.getPlatform().name(), notification.getHoursBefore());
        }

        public Person.Notification getNotification() {
            Person.Notification notification = new Person.Notification();
            try {
                notification.setPlatform(Platform.valueOf(this.platform));
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Platform");
            }
            notification.setHoursBefore(this.hoursBefore);
            if(notification.getHoursBefore() < 0 || notification.getHoursBefore() > 168) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Hours Before, must be between 0 and 168");
            }
            return notification;
        }
    }
}