package de.kjgstbarbara.views;


import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.router.*;
import com.vaadin.flow.spring.security.AuthenticationContext;
import de.kjgstbarbara.data.Organisation;
import de.kjgstbarbara.data.Person;
import de.kjgstbarbara.service.*;
import de.kjgstbarbara.views.nav.MainNavigationView;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.core.userdetails.UserDetails;

@Route(value = "organisation/join/:organisationID", layout = MainNavigationView.class)
@PermitAll
public class JoinOrganisationView extends Div implements BeforeEnterObserver {
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
            if(!organisation.getMembershipRequests().contains(person)) {
                if (!organisation.getMembers().contains(person)) {
                    organisation.getMembershipRequests().add(person);
                    organisationRepository.save(organisation);
                    Notification.show("Deine Beitrittsanfrage für " + organisation.getName() + " wurde verschickt").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
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