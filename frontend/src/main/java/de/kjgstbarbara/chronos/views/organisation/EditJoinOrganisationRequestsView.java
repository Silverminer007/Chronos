package de.kjgstbarbara.chronos.views.organisation;


import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;
import de.kjgstbarbara.chronos.Result;
import de.kjgstbarbara.chronos.data.Organisation;
import de.kjgstbarbara.chronos.data.Person;
import de.kjgstbarbara.chronos.messaging.MessageFormatter;
import de.kjgstbarbara.chronos.service.OrganisationRepository;
import de.kjgstbarbara.chronos.service.OrganisationService;
import de.kjgstbarbara.chronos.service.PersonsRepository;
import de.kjgstbarbara.chronos.service.PersonsService;
import de.kjgstbarbara.chronos.views.MainNavigationView;
import jakarta.annotation.security.PermitAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;

@Route(value = "organisation/manage/:organisationID/:person/:action", layout = MainNavigationView.class)
@PermitAll
public class EditJoinOrganisationRequestsView extends VerticalLayout implements BeforeEnterObserver {
    private static final Logger LOGGER = LoggerFactory.getLogger(EditJoinOrganisationRequestsView.class);
    private final PersonsRepository personsRepository;
    private final OrganisationRepository organisationRepository;

    private final Person person;

    public EditJoinOrganisationRequestsView(PersonsService personsService, OrganisationService organisationService, AuthenticationContext authenticationContext) {
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
        this.removeAll();
        Organisation organisation = beforeEnterEvent.getRouteParameters().get("organisationID").map(Long::valueOf).flatMap(organisationRepository::findById).orElse(null);
        Person requester = beforeEnterEvent.getRouteParameters().get("person").map(Long::valueOf).flatMap(personsRepository::findById).orElse(null);
        String action = beforeEnterEvent.getRouteParameters().get("action").orElse("");
        if (organisation != null && organisation.getAdmin().getId() == person.getId()) {
            if (requester != null && organisation.getMembershipRequests().contains(requester)) {
                if (!organisation.getMembers().contains(requester)) {
                    if ("yes".equals(action)) {
                        organisation.getMembershipRequests().remove(requester);
                        organisation.getMembers().add(requester);
                        organisationRepository.save(organisation);
                        this.add("Die Beitrittsanfrage von " + requester.getName() + " wurde akzeptiert. Sie*er ist jetzt Mitglied deiner Organisation");
                        MessageFormatter messageFormatter = new MessageFormatter().organisation(organisation).person(requester);
                        Result result = organisation.sendMessageTo(messageFormatter.format(
                                """
                                        Hi #PERSON_NAME,
                                        deine Beitrittsanfrage zu #ORGANISATION_NAME wurde akzeptiert. Du kannst jetzt Mitglied von Gruppen dieser Organisation werden um Termine zu sehen und neue Gruppen erstellen
                                        #BASE_URL/groups
                                        """
                        ), requester);
                        if (result.isError()) {
                            LOGGER.error(result.getErrorMessage());
                        }
                    } else {
                        organisation.getMembershipRequests().remove(requester);
                        organisationRepository.save(organisation);
                        this.add("Die Beitrittsanfrage von " + requester.getName() + " wurde abgelehnt");
                        MessageFormatter messageFormatter = new MessageFormatter().organisation(organisation).person(requester);
                        Result result = organisation.sendMessageTo(messageFormatter.format(
                                """
                                        Hi #PERSON_NAME,
                                        deine Beitrittsanfrage zu #ORGANISATION_NAME wurde abgelehnt
                                        """
                        ), requester);
                        if (result.isError()) {
                            LOGGER.error(result.getErrorMessage());
                        }
                    }
                } else {
                    this.add("Diese Person ist schon Mitglied deiner Organisation: " + organisation.getName());
                }
            } else {
                this.add("Es konnte keine Beitrittsanfrage von dieser Person gefunden werden. Wahrscheinlich hat die Person ihren Account gelöscht");
            }
        } else {
            this.add("Du bist nicht berechtigt Beitrittsanfragen zu dieser Organisation zu bearbeiten. Das darf nur sein*e Ersteller*in");
        }

        Button startPage = new Button("Zur Startseite");
        startPage.addClickListener(event -> UI.getCurrent().navigate(""));
        startPage.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        startPage.setIcon(VaadinIcon.HOME.create());
        this.add(startPage);
    }
}