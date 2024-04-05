package de.kjgstbarbara.views.nav;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.*;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.theme.lumo.LumoIcon;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.kjgstbarbara.data.Board;
import de.kjgstbarbara.data.Person;
import de.kjgstbarbara.data.Role;
import de.kjgstbarbara.service.BoardsRepository;
import de.kjgstbarbara.service.BoardsService;
import de.kjgstbarbara.service.PersonsRepository;
import de.kjgstbarbara.service.PersonsService;
import de.kjgstbarbara.views.BoardsView;
import de.kjgstbarbara.views.MyDatesView;
import de.kjgstbarbara.views.OrganisationsView;
import jakarta.annotation.Nullable;
import org.springframework.data.util.Pair;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;
import java.util.function.Consumer;

public abstract class NavigationView extends AppLayout implements RouterLayout, BeforeEnterObserver {
    protected final transient AuthenticationContext authenticationContext;
    protected final BoardsRepository boardsRepository;
    protected final PersonsRepository personsRepository;

    protected Board board;
    private final Person person;

    public NavigationView(BoardsService boardsService, PersonsService personsService, AuthenticationContext authenticationContext) {
        this.boardsRepository = boardsService.getBoardsRepository();
        this.personsRepository = personsService.getPersonsRepository();
        this.authenticationContext = authenticationContext;
        Optional<UserDetails> userDetailsOptional = authenticationContext.getAuthenticatedUser(UserDetails.class);
        this.person = authenticationContext.getAuthenticatedUser(UserDetails.class)
                .flatMap(userDetails -> personsRepository.findByUsername(userDetails.getUsername())).orElse(null);
        if (this.person != null) {
            addToNavbar(createHeaderContent());
            setDrawerOpened(false);
        } else {
            authenticationContext.logout();
        }
    }

    private final Div appName = new Div();
    private final RouterLink back = new RouterLink();
    protected final UnorderedList list = new UnorderedList();
    protected final Header header = new Header();
    protected final Nav nav = new Nav();

    private Component createHeaderContent() {
        header.addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN, LumoUtility.Width.FULL);

        HorizontalLayout horizontalLayout = new HorizontalLayout();
        HorizontalLayout inner = new HorizontalLayout();
        HorizontalLayout layout = new HorizontalLayout();
        layout.setWidth("50%");
        layout.addClassNames(LumoUtility.Display.FLEX, LumoUtility.AlignItems.CENTER, LumoUtility.Padding.Horizontal.LARGE);

        appName.addClassNames(LumoUtility.Margin.Vertical.MEDIUM, LumoUtility.Margin.End.AUTO, LumoUtility.FontSize.LARGE);
        back.add(VaadinIcon.HOME.create());
        layout.add(back, appName);
        layout.setPadding(true);

        horizontalLayout.add(layout);
        horizontalLayout.add(inner);

        MenuBar userMenu = new MenuBar();
        var subMenu = userMenu.addItem(LumoIcon.USER.create()).getSubMenu();
        subMenu.add(menuButton("Meine Termine", LumoIcon.CALENDAR.create(), ui -> ui.navigate(MyDatesView.class)));
        if (this.person.hasAuthority(Role.Type.ADMIN, Role.Scope.ALL, null)) {
            subMenu.add(menuButton("Organisationen", VaadinIcon.OFFICE.create(), ui -> ui.navigate(OrganisationsView.class)));
        }
        subMenu.add(menuButton("Logout", VaadinIcon.EXIT.create(), ui -> authenticationContext.logout()));
        inner.add(userMenu);
        inner.setWidth("50%");
        inner.setPadding(true);
        inner.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        header.add(horizontalLayout);

        nav.addClassNames(LumoUtility.Display.FLEX, LumoUtility.Overflow.AUTO, LumoUtility.Padding.Horizontal.MEDIUM, LumoUtility.Padding.Vertical.XSMALL);

        // Wrap the links in a list; improves accessibility
        list.addClassNames(LumoUtility.Display.FLEX, LumoUtility.Gap.SMALL, LumoUtility.ListStyleType.NONE, LumoUtility.Margin.NONE, LumoUtility.Padding.NONE);
        nav.add(list);
        header.add(nav);

        return header;
    }

    private Button menuButton(String title, @Nullable Icon icon, Consumer<UI> onClick) {
        Button button = new Button(title);
        if (icon != null) {
            button.setIcon(icon);
        }
        button.addClickListener(event -> event.getSource().getUI().ifPresent(onClick));
        button.setWidthFull();
        button.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        return button;
    }

    @Override
    public void afterNavigation() {
        super.afterNavigation();
        nav.setVisible(menu());
        appName.removeAll();
        appName.add(title());
        list.removeAll();
        for (MenuItemInfo menuItem : createMenuItems()) {
            list.add(menuItem);
        }
        back.setRoute(BoardsView.class);
    }

    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        board = beforeEnterEvent.getRouteParameters().get("boardID").map(Long::valueOf).flatMap(boardsRepository::findById).orElse(null);
        if (board == null && boardRequired()) {
            beforeEnterEvent.forwardTo(BoardsView.class);
        }
    }

    abstract MenuItemInfo[] createMenuItems();

    abstract boolean boardRequired();

    abstract Pair<Class<? extends Component>, RouteParameters> back(AuthenticationContext authenticationContext);

    abstract Component title();

    abstract boolean menu();

    public static class MenuItemInfo extends ListItem {

        public MenuItemInfo(String menuTitle, Component icon, Class<? extends Component> view, RouteParameters routeParameters) {
            RouterLink link = new RouterLink();
            // Use Lumo classnames for various styling
            link.addClassNames(LumoUtility.Display.FLEX, LumoUtility.Gap.XSMALL, LumoUtility.Height.MEDIUM, LumoUtility.AlignItems.CENTER, LumoUtility.Padding.Horizontal.SMALL,
                    LumoUtility.TextColor.BODY);
            link.setRoute(view, routeParameters);

            Span text = new Span(menuTitle);
            // Use Lumo classnames for various styling
            text.addClassNames(LumoUtility.FontWeight.MEDIUM, LumoUtility.FontSize.MEDIUM, LumoUtility.Whitespace.NOWRAP);

            if (icon != null) {
                link.add(icon);
            }
            link.add(text);
            add(link);
        }

    }
}
