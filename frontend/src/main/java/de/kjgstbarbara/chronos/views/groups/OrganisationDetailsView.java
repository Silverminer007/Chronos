package de.kjgstbarbara.chronos.views.groups;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.H5;
import com.vaadin.flow.component.html.H6;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.kjgstbarbara.chronos.components.Search;
import de.kjgstbarbara.chronos.data.Organisation;
import de.kjgstbarbara.chronos.data.Person;
import de.kjgstbarbara.chronos.service.OrganisationRepository;
import de.kjgstbarbara.chronos.service.PersonsService;
import de.kjgstbarbara.chronos.views.RegisterView;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

@PermitAll
@Route("organisation/details/:org/:tab?")
public class OrganisationDetailsView extends VerticalLayout implements BeforeEnterObserver {
    private final OrganisationRepository organisationRepository;
    private final Person loggedInUser;

    private Organisation organisation;
    private final H4 title = new H4();
    private Component selectedTab = new OverviewTab();

    public OrganisationDetailsView(OrganisationRepository organisationRepository, PersonsService personsService, AuthenticationContext authenticationContext) {
        this.organisationRepository = organisationRepository;
        this.loggedInUser = authenticationContext.getAuthenticatedUser(OidcUser.class)
                .flatMap(userDetails -> personsService.getPersonsRepository().findByUsername(userDetails.getUserInfo().getEmail()))
                .orElse(null);
        if (loggedInUser == null) {
            UI.getCurrent().navigate(RegisterView.class);
        }
        this.setSizeFull();
        this.setPadding(false);
        this.setSpacing(false);
        this.add(createHeader());
        this.add(selectedTab);
    }

    private Component createHeader() {
        HorizontalLayout header = new HorizontalLayout();
        header.setSpacing(true);
        header.setPadding(true);
        header.setAlignItems(Alignment.CENTER);
        header.setJustifyContentMode(JustifyContentMode.START);
        header.setWidthFull();
        header.addClassNames(LumoUtility.Background.PRIMARY);

        Button back = new Button();
        back.addThemeVariants(ButtonVariant.LUMO_CONTRAST, ButtonVariant.LUMO_TERTIARY_INLINE);
        back.setAriaLabel("Vorherige Seite");
        back.addClickListener(event -> {
            if(this.selectedTab instanceof OverviewTab) {
                UI.getCurrent().navigate(OrganisationListView.class);
            } else {
                Component newTab = new OverviewTab();
                this.replace(this.selectedTab, newTab);
                this.selectedTab = newTab;
            }
        });
        back.setIcon(VaadinIcon.ARROW_LEFT.create());
        header.add(back);

        header.add(new Search(s -> {}));

        header.add(title);
        return header;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        this.organisation = beforeEnterEvent.getRouteParameters().get("org").map(Long::valueOf).flatMap(organisationRepository::findById).orElse(null);
        if (this.organisation == null || !this.organisation.getMembers().contains(this.loggedInUser)) {
            beforeEnterEvent.rerouteTo(OrganisationListView.class);
            return;
        }
        title.setText(this.organisation.getName());
        String tab = beforeEnterEvent.getRouteParameters().get("tab").orElse("");
        Component newTab = switch (tab) {
            case "members" -> new MembersTab();
            case "groups" -> new GroupsTab();
            default -> new OverviewTab();
        };
        this.replace(this.selectedTab, newTab);
        this.selectedTab = newTab;
    }

    private class OverviewTab extends VerticalLayout {
        public OverviewTab() {
            //this.setSizeFull();
            this.setJustifyContentMode(JustifyContentMode.START);
            this.setAlignItems(Alignment.START);
            this.setPadding(true);
            this.setSpacing(true);

            this.add(tabLink("Mitglieder", "members"));
            this.add(tabLink("Gruppen", "groups"));
        }

        private HorizontalLayout tabLink(String name, String route) {
            HorizontalLayout tabLink = new HorizontalLayout();
            tabLink.setWidthFull();
            tabLink.addClassNames(LumoUtility.Background.CONTRAST, LumoUtility.BorderRadius.MEDIUM);
            tabLink.setAlignItems(Alignment.CENTER);
            tabLink.setJustifyContentMode(JustifyContentMode.BETWEEN);
            tabLink.add(new H6(name));
            tabLink.add(VaadinIcon.ARROW_RIGHT.create());
            tabLink.addClickListener(event ->
                    UI.getCurrent().navigate(OrganisationListView.class, new RouteParameters(
                            new RouteParam("org", OrganisationDetailsView.this.organisation.getId()),
                            new RouteParam("tab", route))));
            return tabLink;
        }
    }

    private class MembersTab extends VerticalLayout {
        public MembersTab() {
            this.setSizeFull();
            this.setJustifyContentMode(JustifyContentMode.START);
            this.setAlignItems(Alignment.START);

            this.add(new H5("Mitglieder"));
        }
    }

    private class GroupsTab extends VerticalLayout {
        public GroupsTab() {

        }
    }
}