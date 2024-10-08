package de.kjgstbarbara.chronos.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.IconFactory;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.theme.lumo.Lumo;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.kjgstbarbara.chronos.data.Person;
import de.kjgstbarbara.chronos.service.PersonsService;
import de.kjgstbarbara.chronos.views.dates.CalendarView;
import de.kjgstbarbara.chronos.views.groups.OrganisationListView;
import de.kjgstbarbara.chronos.views.account.ProfileView;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.ArrayList;
import java.util.List;

/**
 * The main view is a top-level placeholder for other views.
 */
public class MainNavigationView extends AppLayout implements BeforeEnterObserver {

    private final Person person;
    private final List<Link> links = new ArrayList<>();

    public MainNavigationView(PersonsService personsService, AuthenticationContext authenticationContext) {
        this.person = authenticationContext.getAuthenticatedUser(OidcUser.class)
                .flatMap(userDetails -> personsService.getPersonsRepository().findByUsername(userDetails.getUserInfo().getEmail()))
                .orElse(null);
        if (person == null) {
            UI.getCurrent().navigate(RegisterView.class);
        } else {
            setPrimarySection(Section.NAVBAR);
            addToNavbar(true, getNavigation());
        }
    }

    private Component wrappedContent;

    @Override
    public void setContent(Component content) {
        HorizontalLayout wrapper = new HorizontalLayout();
        wrapper.setSizeFull();
        wrapper.setPadding(false);
        wrapper.setSpacing(false);
        wrapper.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        wrapper.setAlignItems(FlexComponent.Alignment.CENTER);
        wrapper.add(content);
        this.wrappedContent = content;
        super.setContent(wrapper);
    }

    private HorizontalLayout getNavigation() {
        HorizontalLayout navigation = new HorizontalLayout();
        navigation.addClassNames(LumoUtility.Width.FULL,
                LumoUtility.JustifyContent.EVENLY,
                LumoUtility.AlignSelf.STRETCH);
        navigation.setPadding(false);
        navigation.setSpacing(false);
        navigation.add(
                createLink(VaadinIcon.CALENDAR, "Termine", CalendarView.class),
                createLink(VaadinIcon.GROUP, "Gruppen", OrganisationListView.class),
                createLink(VaadinIcon.COG, "Account", ProfileView.class)
        );

        return navigation;
    }

    private RouterLink createLink(IconFactory icon, String viewName, Class<? extends Component> target) {
        RouterLink link = new RouterLink();
        link.addClassNames(LumoUtility.Display.FLEX,
                LumoUtility.AlignItems.CENTER,
                LumoUtility.Padding.Horizontal.LARGE,
                LumoUtility.TextColor.SECONDARY);
        VerticalLayout entry = new VerticalLayout();
        entry.setAlignItems(FlexComponent.Alignment.CENTER);
        entry.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        entry.setPadding(false);
        entry.setSpacing(false);
        Icon styledIcon = icon.create();
        entry.add(styledIcon);
        entry.add(new NativeLabel(viewName));
        link.add(entry);
        link.getElement().setAttribute("aria-label", viewName);
        link.setRoute(target);
        links.add(new Link(styledIcon, viewName, target));
        return link;
    }

    @Override
    protected void afterNavigation() {
        super.afterNavigation();
        for(Link link : links) {
            if (this.wrappedContent != null && this.wrappedContent.getClass().equals(link.targetView)) {
                link.icon.setColor(Colors.PRIMARY);
            } else {
                link.icon.setColor(Colors.WHITE);
            }
        }
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        if (person == null) {
            beforeEnterEvent.rerouteTo(RegisterView.class);
            return;
        }
        this.setTheme(this.person.isDarkMode());
    }

    private void setTheme(boolean dark) {
        var js = "document.documentElement.setAttribute('theme', $0)";
        getElement().executeJs(js, dark ? Lumo.DARK : Lumo.LIGHT);
    }

    record Link(Icon icon, String viewName, Class<? extends Component> targetView) {
    }
}