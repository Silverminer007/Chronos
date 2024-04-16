package de.kjgstbarbara.views.nav;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Footer;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.kjgstbarbara.data.Person;
import de.kjgstbarbara.service.PersonsService;
import de.kjgstbarbara.views.BoardView;
import de.kjgstbarbara.views.DateView;
import de.kjgstbarbara.views.ProfileView;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * The main view is a top-level placeholder for other views.
 */
public class MainNavigationView extends AppLayout {
    private final transient AuthenticationContext authenticationContext;

    private H1 viewTitle;
    private final Person person;

    public MainNavigationView(PersonsService personsService, AuthenticationContext authenticationContext) {
        this.authenticationContext = authenticationContext;
        this.person = authenticationContext.getAuthenticatedUser(UserDetails.class)
                .flatMap(userDetails -> personsService.getPersonsRepository().findByUsername(userDetails.getUsername()))
                .orElse(null);
        if(person == null) {
            authenticationContext.logout();
        }
        setPrimarySection(Section.DRAWER);
        addDrawerContent();
        addHeaderContent();
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

        HorizontalLayout avatarWrapper = createAvatarButton();

        header.add(title, avatarWrapper);
        addToNavbar(true, toggle, header);
    }

    private HorizontalLayout createAvatarButton() {
        Avatar avatar = this.person.getAvatar();
        HorizontalLayout button = new HorizontalLayout(avatar);
        button.setPadding(true);
        button.addClickListener(event -> event.getSource().getUI().ifPresent(ui -> ui.navigate(ProfileView.class)));
        HorizontalLayout avatarWrapper = new HorizontalLayout(button);
        avatarWrapper.setWidth("50%");
        avatarWrapper.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        avatarWrapper.setAlignItems(FlexComponent.Alignment.END);
        return avatarWrapper;
    }

    private void addDrawerContent() {
        H1 appName = new H1("KjG Termine");
        appName.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);
        Header header = new Header(appName);

        Scroller scroller = new Scroller(createNavigation());

        addToDrawer(header, scroller, createFooter());
    }

    private SideNav createNavigation() {
        SideNav nav = new SideNav();

        nav.addItem(new SideNavItem("Meine Termine", DateView.class, VaadinIcon.CALENDAR_USER.create()));
        nav.addItem(new SideNavItem("Meine Boards", BoardView.class, VaadinIcon.GROUP.create()));

        return nav;
    }

    private Footer createFooter() {
        Footer layout = new Footer();

        Button logOut = new Button("Abmelden", VaadinIcon.EXIT.create());
        logOut.addClickListener(buttonClickEvent -> authenticationContext.logout());
        logOut.setWidthFull();
        layout.add(logOut);
        return layout;
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
}