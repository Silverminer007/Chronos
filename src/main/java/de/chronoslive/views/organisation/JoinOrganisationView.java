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

@Route(value = "organisation/join/:organisationID", layout = MainNavigationView.class)
@PermitAll
public class JoinOrganisationView extends VerticalLayout implements BeforeEnterObserver {
    private final OrganisationRepository organisationRepository;

    private final Person person;

    public JoinOrganisationView(PersonsService personsService, OrganisationService organisationService) {
        PersonsRepository personsRepository = personsService.getPersonsRepository();
        this.organisationRepository = organisationService.getOrganisationRepository();
        this.person = FrontendUtils.getAuthenticatedUser(personsRepository).orElse(null);
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
                    new MessageFormatter(organisation.getAdmin()).organisation(organisation).person(person).send(Messages.ORGANISATION_JOIN_REQUEST_NEW);
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
        startPage.addClickListener(event ->
                UI.getCurrent().navigate(""));
        startPage.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        startPage.setIcon(VaadinIcon.HOME.create());
        this.add(startPage);
    }
}