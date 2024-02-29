package de.kjgstbarbara.views.nav;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.*;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.theme.lumo.LumoIcon;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.kjgstbarbara.data.Board;
import de.kjgstbarbara.data.Organisation;
import de.kjgstbarbara.service.*;
import de.kjgstbarbara.views.BoardsView;
import de.kjgstbarbara.views.MyDatesView;
import de.kjgstbarbara.views.OrganisationsView;
import org.springframework.data.util.Pair;

public abstract class NavigationView extends AppLayout implements RouterLayout, BeforeEnterObserver {
    protected final transient AuthenticationContext authenticationContext;
    protected final OrganisationRepository organisationRepository;
    protected final BoardsRepository boardsRepository;
    protected final PersonsRepository personsRepository;

    protected Board board;
    protected Organisation organisation;

    public NavigationView(OrganisationsService organisationsService, BoardsService boardsService, PersonsService personsService, AuthenticationContext authenticationContext) {
        this.organisationRepository = organisationsService.getOrganisationRepository();
        this.boardsRepository = boardsService.getBoardsRepository();
        this.personsRepository = personsService.getPersonsRepository();
        this.authenticationContext = authenticationContext;
        addToNavbar(createHeaderContent());
        setDrawerOpened(false);
    }

    private final H1 appName = new H1();
    private final RouterLink back = new RouterLink();
    protected final UnorderedList list = new UnorderedList();
    protected final Header header = new Header();
    protected final Nav nav = new Nav();

    private Component createHeaderContent() {
        header.addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN, LumoUtility.Width.FULL);

        HorizontalLayout horizontalLayout = new HorizontalLayout();
        HorizontalLayout inner = new HorizontalLayout();
        Div layout = new Div();
        layout.setWidth("50%");
        layout.addClassNames(LumoUtility.Display.FLEX, LumoUtility.AlignItems.CENTER, LumoUtility.Padding.Horizontal.LARGE);

        appName.addClassNames(LumoUtility.Margin.Vertical.MEDIUM, LumoUtility.Margin.End.AUTO, LumoUtility.FontSize.LARGE);
        back.add(LumoIcon.ARROW_LEFT.create());
        layout.add(back, appName);

        horizontalLayout.add(layout);
        horizontalLayout.add(inner);

        MenuBar userMenu = new MenuBar();
        userMenu.addItem(LumoIcon.USER.create()).getSubMenu().add(
                new RouterLink("Meine Termine", MyDatesView.class, new RouteParameters(new RouteParam("person", "1")))// TODO Styling
        );
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

    private record MenuOption(String text, Class<? extends Component> target, RouteParameters routeParameters) {
        public String toString() {
            return text;
        }

    }

    @Override
    public void afterNavigation() {
        super.afterNavigation();
        nav.setVisible(menu());
        appName.setText(title());
        list.removeAll();
        for (MenuItemInfo menuItem : createMenuItems()) {
            list.add(menuItem);
        }
        Pair<Class<? extends Component>, RouteParameters> backRoute = back(authenticationContext);
        back.setVisible(backRoute != null);
        if (backRoute != null) {
            back.setRoute(backRoute.getFirst(), backRoute.getSecond());
        }
    }

    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        organisation = beforeEnterEvent.getRouteParameters().get("orgID").flatMap(organisationRepository::findById).orElse(null);
        board = beforeEnterEvent.getRouteParameters().get("boardID").map(Long::valueOf).flatMap(boardsRepository::findById).orElse(null);
        if (organisation == null && orgRequired()) {
            beforeEnterEvent.forwardTo(OrganisationsView.class);
        } else if (board == null && boardRequired()) {
            beforeEnterEvent.forwardTo(BoardsView.class, new RouteParameters("orgID", organisation.getId()));
        }
    }

    abstract MenuItemInfo[] createMenuItems();

    abstract boolean orgRequired();

    abstract boolean boardRequired();

    abstract Pair<Class<? extends Component>, RouteParameters> back(AuthenticationContext authenticationContext);

    abstract String title();

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
