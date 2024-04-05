package de.kjgstbarbara.views.nav;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.router.RouteParam;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.theme.lumo.LumoIcon;
import de.kjgstbarbara.service.BoardsService;
import de.kjgstbarbara.service.PersonsService;
import de.kjgstbarbara.views.BoardsView;
import de.kjgstbarbara.views.modules.DatesView;
import de.kjgstbarbara.views.modules.MembersView;
import jakarta.annotation.security.PermitAll;
import org.springframework.data.util.Pair;
import org.vaadin.lineawesome.LineAwesomeIcon;

@PermitAll
public class ModuleNavigationView extends NavigationView {

    public ModuleNavigationView(BoardsService boardsService, PersonsService personsService, AuthenticationContext authenticationContext) {
        super(boardsService, personsService, authenticationContext);
    }

    @Override
    MenuItemInfo[] createMenuItems() {
        RouteParameters routeParameters = new RouteParameters(new RouteParam("boardID", board.getId()));
        return new MenuItemInfo[]{
                new MenuItemInfo(board.getMemberTitle(), LineAwesomeIcon.PERSON_BOOTH_SOLID.create(), MembersView.class, routeParameters),
                new MenuItemInfo(board.getDateTitle(), LumoIcon.CALENDAR.create(), DatesView.class, routeParameters)
        };
    }

    @Override
    boolean boardRequired() {
        return true;
    }

    @Override
    Pair<Class<? extends Component>, RouteParameters> back(AuthenticationContext authenticationContext) {
        return Pair.of(BoardsView.class, new RouteParameters());
    }

    @Override
    Component title() {
        return new H1(board.getTitle());
    }

    @Override
    boolean menu() {
        return true;
    }
}
