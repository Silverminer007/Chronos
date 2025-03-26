package de.kjgstbarbara;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.avatar.AvatarGroup;
import com.vaadin.flow.server.StreamResource;
import de.kjgstbarbara.data.Date;
import de.kjgstbarbara.data.Feedback;
import de.kjgstbarbara.data.Person;
import de.kjgstbarbara.service.PersonsRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.List;
import java.util.Optional;

public class FrontendUtils {
    public static Avatar getAvatar(Person person) {
        Avatar avatar = new Avatar(person.getName());
        StreamResource pp = FileHelper.getProfileImage(person);
        avatar.setImageResource(pp);
        return avatar;
    }

    public static AvatarGroup.AvatarGroupItem getAvatarGroupItem(Person person) {
        AvatarGroup.AvatarGroupItem avatarGroupItem = new AvatarGroup.AvatarGroupItem(person.getName());
        StreamResource pp = FileHelper.getProfileImage(person);
        avatarGroupItem.setImageResource(pp);
        return avatarGroupItem;
    }

    public static List<AvatarGroup.AvatarGroupItem> getAvatars(Date date, Feedback.Status status) {
        return date.getGroup().getMembers().stream().filter(p -> date.getStatusFor(p).equals(status)).map(FrontendUtils::getAvatarGroupItem).toList();
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
