package de.chronoslive.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.IconFactory;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.theme.lumo.Lumo;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.chronoslive.utility.FrontendUtils;
import de.chronoslive.entitys.Person;
import de.chronoslive.services.PersonsService;
import de.chronoslive.views.date.calendar.CalendarView;
import de.chronoslive.views.organisation.NewOrganisationView;
import de.chronoslive.views.settings.SettingsView;
import jakarta.annotation.security.PermitAll;

import java.util.ArrayList;
import java.util.List;

/**
 * The main view is a top-level placeholder for other views.
 */
@PermitAll
public class MainNavigationView extends AppLayout implements BeforeEnterObserver, AfterNavigationObserver {

    private final Person person;
    private final List<Link> links = new ArrayList<>();

    public MainNavigationView(PersonsService personsService) {
        this.person = FrontendUtils.getAuthenticatedUser(personsService.getPersonsRepository()).orElse(null);
        setPrimarySection(Section.NAVBAR);
        addToNavbar(true, getNavigation());
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
                createLink(VaadinIcon.GROUP, "Gruppen", NewOrganisationView.class),
                createLink(VaadinIcon.COG, "Einstellungen", SettingsView.class)
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
    public void afterNavigation(AfterNavigationEvent event) {
        for(Link link : links) {
            if (this.wrappedContent != null && this.wrappedContent.getClass().equals(link.targetView)) {
                link.icon.setColor("#ff0000");
            } else {
                link.icon.setColor("#ffffff");
            }
        }
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        if (person == null) {
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