package de.kjgstbarbara.chronos.views.groups;


import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;
import com.vaadin.flow.spring.security.AuthenticationContext;
import de.kjgstbarbara.chronos.Result;
import de.kjgstbarbara.chronos.data.Organisation;
import de.kjgstbarbara.chronos.data.Person;
import de.kjgstbarbara.chronos.messaging.MessageFormatter;
import de.kjgstbarbara.chronos.service.*;
import de.kjgstbarbara.chronos.views.MainNavigationView;
import de.kjgstbarbara.chronos.views.RegisterView;
import jakarta.annotation.security.PermitAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

@Route(value = "organisation/join/:organisationID", layout = MainNavigationView.class)
@PermitAll
public class JoinOrganisationView extends VerticalLayout implements BeforeEnterObserver {
    private static final Logger LOGGER = LoggerFactory.getLogger(JoinOrganisationView.class);
    private final PersonsRepository personsRepository;
    private final OrganisationRepository organisationRepository;

    private final Person person;

    public JoinOrganisationView(PersonsService personsService, OrganisationService organisationService, AuthenticationContext authenticationContext) {
        this.personsRepository = personsService.getPersonsRepository();
        this.organisationRepository = organisationService.getOrganisationRepository();
        this.person = authenticationContext.getAuthenticatedUser(OidcUser.class)
                .flatMap(userDetails -> personsRepository.findByUsername(userDetails.getUserInfo().getEmail()))
                .orElse(null);
        if (person == null) {
            UI.getCurrent().navigate(RegisterView.class);
        }
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        this.removeAll();
        Organisation organisation = beforeEnterEvent.getRouteParameters().get("organisationID").map(Long::valueOf).flatMap(organisationRepository::findById).orElse(null);
        if (organisation != null) {
            if (!organisation.getMembershipRequests().contains(person)) {
                if (!organisation.getMembers().contains(person)) {
                    organisation.getMembershipRequests().add(person);
                    organisationRepository.save(organisation);
                    this.add("Deine Beitrittsanfrage für " + organisation.getName() + " wurde verschickt");
                        MessageFormatter messageFormatter = new MessageFormatter().organisation(organisation).person(person);
                        Result result = organisation.sendMessageTo(messageFormatter.format(
                                """
                                        Hi #ORGANISATION_ADMIN_NAME,
                                        #PERSON_NAME möchte gerne deiner Organisation #ORGANISATION_NAME beitreten. Wenn du diese Anfrage bearbeiten möchtest, klicke bitte auf diesen Link
                                        
                                        Anfrage annehmen: #BASE_URL/organisation/manage/#ORGANISATION_ID/#PERSON_ID/yes
                                        
                                        Anfrage ablehnen: #BASE_URL/organisation/manage/#ORGANISATION_ID/#PERSON_ID/no
                                        """
                        ), organisation.getAdmin());
                        if(result.isError()) {
                            LOGGER.error(result.getErrorMessage());
                            Notification.show("Der Organisationsadmin konnte nicht informiert werden, dass du eine Beitrittsanfrage gestellt hast. Bitte informiere den Entwickler des Programms");
                        }
                } else {
                    this.add("Du gehörst schon zu dieser Organisation: " + organisation.getName());
                }
            } else {
                this.add("Deine Beitrittsanfrage zu dieser Organisation läuft noch: " + organisation.getName());
            }
        } else {
            this.add("Die Organisation existiert nicht mehr. Bitte frage nach einem neuen Beitrittslink");
        }

        Button startPage = new Button("Zur Startseite");
        startPage.addClickListener(event -> {
            UI.getCurrent().navigate("");
        });
        startPage.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        startPage.setIcon(VaadinIcon.HOME.create());
        this.add(startPage);
    }
}