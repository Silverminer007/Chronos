package de.kjgstbarbara.views.nav;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.html.Footer;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.theme.lumo.Lumo;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.kjgstbarbara.data.Person;
import de.kjgstbarbara.gameevaluation.views.GameEvaluationListView;
import de.kjgstbarbara.service.PersonsRepository;
import de.kjgstbarbara.service.PersonsService;
import de.kjgstbarbara.views.*;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * The main view is a top-level placeholder for other views.
 */
public class MainNavigationView extends AppLayout implements BeforeEnterObserver {
    private final transient AuthenticationContext authenticationContext;
    private final PersonsRepository personsRepository;

    private H1 viewTitle;
    private final Person person;

    public MainNavigationView(PersonsService personsService, AuthenticationContext authenticationContext) {
        this.authenticationContext = authenticationContext;
        this.personsRepository = personsService.getPersonsRepository();
        this.person = authenticationContext.getAuthenticatedUser(UserDetails.class)
                .flatMap(userDetails -> personsService.getPersonsRepository().findByUsername(userDetails.getUsername()))
                .orElse(null);
        if (person == null) {
            authenticationContext.logout();
        } else {
            setPrimarySection(Section.DRAWER);
            addDrawerContent();
            addHeaderContent();
        }
    }

    private void addHeaderContent() {
        DrawerToggle toggle = new DrawerToggle();
        toggle.setAriaLabel("Menu toggle");

        viewTitle = new H1();
        viewTitle.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);

        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();

        HorizontalLayout title = new HorizontalLayout(viewTitle);
        title.setWidth("50%");
        title.setAlignItems(FlexComponent.Alignment.CENTER);

        Component avatarWrapper = createAvatarButton();

        header.add(title, avatarWrapper);
        addToNavbar(true, toggle, header);
    }

    private Component createAvatarButton() {
        HorizontalLayout wrapper = new HorizontalLayout();
        wrapper.setWidthFull();
        wrapper.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        wrapper.setAlignItems(FlexComponent.Alignment.CENTER);
        wrapper.setPadding(true);

        MenuBar profile = new MenuBar();
        profile.addThemeVariants(MenuBarVariant.LUMO_TERTIARY_INLINE);

        MenuItem personal = profile.addItem(this.person.getAvatar());
        SubMenu personalSubmenu = personal.getSubMenu();

        personalSubmenu.addItem("Profil", event -> UI.getCurrent().navigate(ProfileView.class));
        personalSubmenu.addItem("Benachrichtigungen", event -> UI.getCurrent().navigate(NotificationSettingsView.class));
        personalSubmenu.addItem(this.person.isDarkMode() ? "Heller Modus" : "Dunkler Modus", event -> {
            this.person.setDarkMode(!this.person.isDarkMode());
            personsRepository.save(this.person);
            UI.getCurrent().getPage().reload();
        });
        personalSubmenu.addItem("Abmelden", event -> authenticationContext.logout());

        wrapper.add(profile);
        return wrapper;
    }

    private void addDrawerContent() {
        H1 appName = new H1("Chronos");
        appName.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);
        Header header = new Header(appName);

        Scroller scroller = new Scroller(createNavigation());

        addToDrawer(header, scroller, createFooter());
    }

    private SideNav createNavigation() {
        SideNav nav = new SideNav();

        nav.addItem(new SideNavItem("Meine Termine", CalendarView.class, VaadinIcon.CALENDAR_USER.create()));
        nav.addItem(new SideNavItem("Gruppen", GroupView.class, VaadinIcon.GROUP.create()));
        nav.addItem(new SideNavItem("Organisationen", OrganisationView.class, VaadinIcon.OFFICE.create()));
        nav.addItem(new SideNavItem("Spieleauswertung", GameEvaluationListView.class, VaadinIcon.GAMEPAD.create()));

        return nav;
    }

    private Footer createFooter() {
        return new Footer();
    }

    @Override
    protected void afterNavigation() {
        super.afterNavigation();
        viewTitle.setText(getCurrentPageTitle());
    }

    private String getCurrentPageTitle() {
        PageTitle title = getContent().getClass().getAnnotation(PageTitle.class);
        return title == null ? "" : title.value();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        this.setTheme(this.person.isDarkMode());
    }

    private void setTheme(boolean dark) {
        var js = "document.documentElement.setAttribute('theme', $0)";
        getElement().executeJs(js, dark ? Lumo.DARK : Lumo.LIGHT);
    }
}