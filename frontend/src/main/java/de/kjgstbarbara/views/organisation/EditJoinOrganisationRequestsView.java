package de.kjgstbarbara.views.organisation;


import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;
import de.kjgstbarbara.data.Organisation;
import de.kjgstbarbara.data.Person;
import de.kjgstbarbara.messaging.MessageSender;
import de.kjgstbarbara.messaging.Messages;
import de.kjgstbarbara.service.OrganisationRepository;
import de.kjgstbarbara.service.OrganisationService;
import de.kjgstbarbara.service.PersonsRepository;
import de.kjgstbarbara.service.PersonsService;
import de.kjgstbarbara.views.MainNavigationView;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.core.userdetails.UserDetails;

@Route(value = "organisation/manage/:organisationID/:person/:action", layout = MainNavigationView.class)
@PermitAll
public class EditJoinOrganisationRequestsView extends VerticalLayout implements BeforeEnterObserver {
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
                        new MessageSender(requester).organisation(organisation).person(requester).send(Messages.ORGANISATION_JOIN_REQUEST_ACCEPTED);
                    } else {
                        organisation.getMembershipRequests().remove(requester);
                        organisationRepository.save(organisation);
                        this.add("Die Beitrittsanfrage von " + requester.getName() + " wurde abgelehnt");
                        new MessageSender(requester).organisation(organisation).person(requester).send(Messages.ORGANISATION_JOIN_REQUEST_DECLINED);
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