package de.kjgstbarbara.views;


import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.router.*;
import com.vaadin.flow.spring.security.AuthenticationContext;
import de.kjgstbarbara.FriendlyError;
import de.kjgstbarbara.Utility;
import de.kjgstbarbara.data.Organisation;
import de.kjgstbarbara.data.Person;
import de.kjgstbarbara.messaging.MessageFormatter;
import de.kjgstbarbara.service.*;
import de.kjgstbarbara.views.nav.MainNavigationView;
import jakarta.annotation.security.PermitAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;

@Route(value = "organisation/join/:organisationID", layout = MainNavigationView.class)
@PermitAll
public class JoinOrganisationView extends Div implements BeforeEnterObserver {
    private static final Logger LOGGER = LoggerFactory.getLogger(JoinOrganisationView.class);
    private final PersonsRepository personsRepository;
    private final OrganisationRepository organisationRepository;

    private final Person person;

    public JoinOrganisationView(PersonsService personsService, OrganisationService organisationService, AuthenticationContext authenticationContext) {
        this.personsRepository = personsService.getPersonsRepository();
        this.organisationRepository = organisationService.getOrganisationRepository();
        this.person = authenticationContext.getAuthenticatedUser(UserDetails.class)
                .flatMap(userDetails -> personsRepository.findByUsername(userDetails.getUsername()))
                .orElse(null);
        if (person == null) {
            authenticationContext.logout();
        }
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        Organisation organisation = beforeEnterEvent.getRouteParameters().get("organisationID").map(Long::valueOf).flatMap(organisationRepository::findById).orElse(null);
        if (organisation != null) {
            if (!organisation.getMembershipRequests().contains(person)) {
                if (!organisation.getMembers().contains(person)) {
                    organisation.getMembershipRequests().add(person);
                    organisationRepository.save(organisation);
                    Notification.show("Deine Beitrittsanfrage für " + organisation.getName() + " wurde verschickt").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    UI.getCurrent().getPage().fetchCurrentURL(url -> {
                        MessageFormatter messageFormatter = new MessageFormatter().organisation(organisation).person(person);
                        String newUrl = Utility.baseURL(url) + "/organisations";
                        organisation.sendMessageTo(messageFormatter.format(
                                """
                                        Hi #ORGANISATION_ADMIN_NAME,
                                        #PERSON_NAME möchte gerne deiner Organisation #ORGANISATION_NAME beitreten. Wenn du diese Anfrage bearbeiten möchtest, klicke bitte auf diesen Link
                                        
                                        """ + newUrl
                        ), organisation.getAdmin());
                    });
                } else {
                    Notification.show("Du gehörst schon zu dieser Organisation: " + organisation.getName()).addThemeVariants(NotificationVariant.LUMO_WARNING);
                }
            } else {
                Notification.show("Deine Beitrittsanfrage zu dieser Organisation läuft noch: " + organisation.getName());
            }
        }
        beforeEnterEvent.rerouteTo(OrganisationView.class);
    }
}