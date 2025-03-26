package de.chronoslive.views.organisation;


import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import de.chronoslive.utility.FrontendUtils;
import de.chronoslive.entitys.Organisation;
import de.chronoslive.entitys.Person;
import de.chronoslive.messaging.MessageFormatter;
import de.chronoslive.messaging.Messages;
import de.chronoslive.repositorys.OrganisationRepository;
import de.chronoslive.services.OrganisationService;
import de.chronoslive.repositorys.PersonsRepository;
import de.chronoslive.services.PersonsService;
import de.chronoslive.views.MainNavigationView;
import jakarta.annotation.security.PermitAll;

@Route(value = "organisation/manage/:organisationID/:person/:action", layout = MainNavigationView.class)
@PermitAll
public class EditJoinOrganisationRequestsView extends VerticalLayout implements BeforeEnterObserver {
    private final PersonsRepository personsRepository;
    private final OrganisationRepository organisationRepository;

    private final Person person;

    public EditJoinOrganisationRequestsView(PersonsService personsService, OrganisationService organisationService) {
        this.personsRepository = personsService.getPersonsRepository();
        this.organisationRepository = organisationService.getOrganisationRepository();
        this.person = FrontendUtils.getAuthenticatedUser(this.personsRepository).orElse(null);
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
                        new MessageFormatter(requester).organisation(organisation).person(requester).send(Messages.ORGANISATION_JOIN_REQUEST_ACCEPTED);
                    } else {
                        organisation.getMembershipRequests().remove(requester);
                        organisationRepository.save(organisation);
                        this.add("Die Beitrittsanfrage von " + requester.getName() + " wurde abgelehnt");
                        new MessageFormatter(requester).organisation(organisation).person(requester).send(Messages.ORGANISATION_JOIN_REQUEST_DECLINED);
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