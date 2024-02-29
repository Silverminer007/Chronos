package de.kjgstbarbara.views.nav;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.spring.security.AuthenticationContext;
import de.kjgstbarbara.security.SecurityUtils;
import de.kjgstbarbara.service.BoardsService;
import de.kjgstbarbara.service.OrganisationsService;
import de.kjgstbarbara.service.PersonsService;
import de.kjgstbarbara.views.OrganisationsView;
import jakarta.annotation.security.PermitAll;
import org.springframework.data.util.Pair;
import org.springframework.security.core.userdetails.UserDetails;

@PermitAll
public class BoardsNavigationView extends NavigationView {
    public BoardsNavigationView(OrganisationsService organisationsService, BoardsService boardsService, PersonsService personsService, AuthenticationContext authenticationContext) {
        super(organisationsService, boardsService, personsService, authenticationContext);
    }

    @Override
    MenuItemInfo[] createMenuItems() {
        return new MenuItemInfo[0];
    }

    @Override
    boolean orgRequired() {
        return true;
    }

    @Override
    boolean boardRequired() {
        return false;
    }

    @Override
    Pair<Class<? extends Component>, RouteParameters> back(AuthenticationContext authenticationContext) {
        if(authenticationContext.getAuthenticatedUser(UserDetails.class).map(SecurityUtils::isGlobalAdmin).orElse(false)) {
            return Pair.of(OrganisationsView.class, new RouteParameters());
        } else {
            return null;
        }
    }

    @Override
    String title() {
        return organisation.getName();
    }

    @Override
    boolean menu() {
        return false;
    }
}
