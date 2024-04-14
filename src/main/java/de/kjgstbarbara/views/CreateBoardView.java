package de.kjgstbarbara.views;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;
import de.kjgstbarbara.data.Board;
import de.kjgstbarbara.data.Person;
import de.kjgstbarbara.service.BoardsRepository;
import de.kjgstbarbara.service.BoardsService;
import de.kjgstbarbara.service.PersonsRepository;
import de.kjgstbarbara.service.PersonsService;
import de.kjgstbarbara.views.BoardView;
import de.kjgstbarbara.views.components.BoardWidget;
import de.kjgstbarbara.views.nav.MainNavigationView;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.core.userdetails.UserDetails;

@Route(value = "boards/create", layout = MainNavigationView.class)
@PageTitle("Board erstellen")
@PermitAll
public class CreateBoardView extends VerticalLayout {
    private final PersonsRepository personsRepository;
    private final BoardsRepository boardsRepository;

    private final Person person;

    public CreateBoardView(PersonsService personsService, BoardsService boardsService, AuthenticationContext authenticationContext) {
        this.personsRepository = personsService.getPersonsRepository();
        this.boardsRepository = boardsService.getBoardsRepository();
        this.person = authenticationContext.getAuthenticatedUser(UserDetails.class)
                .flatMap(userDetails -> personsRepository.findByUsername(userDetails.getUsername()))
                .orElse(null);
        if(person == null) {
            authenticationContext.logout();
        }

        this.setHeightFull();
        TextField boardTitle = new TextField("Name des Boards");
        Button back = new Button("Zurück");
        back.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        back.addClickListener(event -> event.getSource().getUI().ifPresent(ui -> ui.navigate(BoardView.class)));
        Button confirm = new Button("Board erstellen");
        confirm.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        confirm.addClickShortcut(Key.ENTER);
        confirm.addClickListener(event ->  {
            if(boardTitle.getValue().isBlank()) {
                boardTitle.setInvalid(true);
                boardTitle.setErrorMessage("Dieses Feld ist erforderlich");
            } else {
                Board newBoard = new Board();
                newBoard.setTitle(boardTitle.getValue());
                newBoard.getMembers().add(person);
                newBoard.getAdmins().add(person);
                boardsRepository.save(newBoard);
                System.out.println(boardsRepository.findAll());
                event.getSource().getUI().ifPresent(ui -> ui.navigate(BoardView.class));
            }
        });
        HorizontalLayout buttons = new HorizontalLayout(back, confirm);
        VerticalLayout wrapper = new VerticalLayout(boardTitle, buttons);
        this.setJustifyContentMode(JustifyContentMode.CENTER);
        this.setAlignItems(Alignment.CENTER);
        this.add(wrapper);

    }
}