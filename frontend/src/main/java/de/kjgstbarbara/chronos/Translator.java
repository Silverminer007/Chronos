package de.kjgstbarbara.chronos;

import com.vaadin.flow.spring.security.AuthenticationContext;
import de.kjgstbarbara.chronos.data.Person;
import de.kjgstbarbara.chronos.service.PersonsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.ResourceBundle;

@Service
public class Translator {
    public static final Translator DEFAULT = new Translator();
    private final ResourceBundle resourceBundle;

    private Translator() {
        this.resourceBundle = ResourceBundle.getBundle("lang.bundle");
    }

    public Translator(AuthenticationContext authenticationContext, PersonsService personsService) {
        resourceBundle = ResourceBundle.getBundle("lang.bundle", authenticationContext.getAuthenticatedUser(UserDetails.class)
                .flatMap(userDetails -> personsService.getPersonsRepository().findByUsername(userDetails.getUsername()).map(Person::getUserLocale))
                .orElse(Locale.US));
    }

    public String translate(String key) {
        return resourceBundle.getString(key);
    }
}
