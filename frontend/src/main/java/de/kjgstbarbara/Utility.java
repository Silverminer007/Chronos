package de.kjgstbarbara;

import com.vaadin.flow.component.UI;
import de.kjgstbarbara.data.Person;
import de.kjgstbarbara.service.PersonsRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.net.URL;
import java.util.Optional;

public class Utility {
    public static String baseURL(URL url) {
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(url.getProtocol()).append("://");
        urlBuilder.append(url.getHost());
        if (url.getPort() != -1) {
            urlBuilder.append(":").append(url.getPort());
        }
        return urlBuilder.toString();
    }

    private static Person getAuthenticatedUser(OidcUser oidcUser, PersonsRepository personsRepository) {
        return personsRepository.findByUsernameOrEmail(oidcUser.getPreferredUsername())
                .orElseGet(() -> {
                    Person person = new Person();
                    person.setUsername(oidcUser.getPreferredUsername());
                    person.setEMailAddress(oidcUser.getEmail());
                    person.setFirstName(oidcUser.getGivenName());
                    person.setLastName(oidcUser.getFamilyName());
                    return personsRepository.save(person);
                });
    }

    public static Optional<Person> getAuthenticatedUser(PersonsRepository personsRepository) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication.getPrincipal() instanceof OidcUser oidcUser) {
            return Optional.ofNullable(getAuthenticatedUser(oidcUser, personsRepository));
        }
        return Optional.empty();
    }

    public static void logout() {// TODO Das ist suboptimal, wenn jemand seinen Account löscht, denn dann kann die Person eingeloggt bleiben, auch wenn der Account gelöscht wird
        UI.getCurrent().getPage().open("/logout", "_self");
    }
}
