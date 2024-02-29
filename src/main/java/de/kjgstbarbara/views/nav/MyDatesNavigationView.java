package de.kjgstbarbara.views.nav;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.spring.security.AuthenticationContext;
import de.kjgstbarbara.service.BoardsService;
import de.kjgstbarbara.service.OrganisationsService;
import de.kjgstbarbara.service.PersonsService;
import jakarta.annotation.security.PermitAll;
import org.springframework.data.util.Pair;

@PermitAll
public class MyDatesNavigationView extends NavigationView {
    public MyDatesNavigationView(OrganisationsService organisationsService, BoardsService boardsService, PersonsService personsService, AuthenticationContext authenticationContext) {
        super(organisationsService, boardsService, personsService, authenticationContext);
    }

    @Override
    MenuItemInfo[] createMenuItems() {
        return new MenuItemInfo[0];
    }

    @Override
    boolean orgRequired() {
        return false;
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
    String title() {
        return "Meine Termine";
    }

    @Override
    boolean menu() {
        return false;
    }
}
