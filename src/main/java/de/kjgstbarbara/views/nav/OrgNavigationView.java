package de.kjgstbarbara.views.nav;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.router.*;
import com.vaadin.flow.spring.security.AuthenticationContext;
import de.kjgstbarbara.service.BoardsService;
import de.kjgstbarbara.service.PersonsService;
import jakarta.annotation.security.PermitAll;
import org.springframework.data.util.Pair;

@PermitAll
public class OrgNavigationView extends NavigationView {
    public OrgNavigationView(BoardsService boardsService, PersonsService personsService, AuthenticationContext authenticationContext) {
        super(boardsService, personsService, authenticationContext);
    }

    @Override
    MenuItemInfo[] createMenuItems() {
        return new MenuItemInfo[0];
    }

    @Override
    boolean boardRequired() {
        return false;
    }

    @Override
    Pair<Class<? extends Component>, RouteParameters> back(AuthenticationContext authenticationContext) {
        return null;
    }

    @Override
    Component title() {
        return new H1("Organisationen");
    }

    @Override
    boolean menu() {
        return false;
    }
}
