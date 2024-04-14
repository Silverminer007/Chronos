package de.kjgstbarbara.views;


import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;
import com.vaadin.flow.spring.security.AuthenticationContext;
import de.kjgstbarbara.data.Board;
import de.kjgstbarbara.data.Person;
import de.kjgstbarbara.service.BoardsRepository;
import de.kjgstbarbara.service.BoardsService;
import de.kjgstbarbara.service.PersonsRepository;
import de.kjgstbarbara.service.PersonsService;
import de.kjgstbarbara.views.components.BoardWidget;
import de.kjgstbarbara.views.nav.MainNavigationView;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.core.userdetails.UserDetails;

@Route(value = "boards/join/:boardID", layout = MainNavigationView.class)
@PermitAll
public class JoinBoardView extends Div implements BeforeEnterObserver {
    private final PersonsRepository personsRepository;
    private final BoardsRepository boardsRepository;

    private final Person person;

    public JoinBoardView(PersonsService personsService, BoardsService boardsService, AuthenticationContext authenticationContext) {
        this.personsRepository = personsService.getPersonsRepository();
        this.boardsRepository = boardsService.getBoardsRepository();
        this.person = authenticationContext.getAuthenticatedUser(UserDetails.class)
                .flatMap(userDetails -> personsRepository.findByUsername(userDetails.getUsername()))
                .orElse(null);
        if (person == null) {
            authenticationContext.logout();
        }
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        Board board = beforeEnterEvent.getRouteParameters().get("boardID").map(Long::valueOf).flatMap(boardsRepository::findById).orElse(null);
        if (board != null) {
            if (!board.getMembers().contains(person)) {
                board.getMembers().add(person);
                boardsRepository.save(board);
            }
        }
        beforeEnterEvent.rerouteTo("");
    }
}