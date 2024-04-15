package de.kjgstbarbara.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
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
import de.kjgstbarbara.views.components.BoardWidget;
import de.kjgstbarbara.views.nav.MainNavigationView;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.core.userdetails.UserDetails;

@Route(value = "boards", layout = MainNavigationView.class)
@PageTitle("Meine Boards")
@PermitAll
public class BoardView extends VerticalLayout implements AfterNavigationObserver {
    private final PersonsRepository personsRepository;
    private final BoardsRepository boardsRepository;

    private final Person person;
    private final Grid<Board> grid = new Grid<>(Board.class, false);

    public BoardView(PersonsService personsService, BoardsService boardsService, AuthenticationContext authenticationContext) {
        this.personsRepository = personsService.getPersonsRepository();
        this.boardsRepository = boardsService.getBoardsRepository();
        this.person = authenticationContext.getAuthenticatedUser(UserDetails.class)
                .flatMap(userDetails -> personsRepository.findByUsername(userDetails.getUsername()))
                .orElse(null);
        if(person == null) {
            authenticationContext.logout();
        }

        this.setHeightFull();
        grid.setHeightFull();
        grid.addComponentColumn(board -> new BoardWidget(board, this.person));
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_ROW_STRIPES);
        grid.setSelectionMode(Grid.SelectionMode.NONE);

        HorizontalLayout footer = new HorizontalLayout();
        HorizontalLayout leftFooter = new HorizontalLayout();
        leftFooter.setWidth("50%");
        HorizontalLayout rightFooter = new HorizontalLayout();
        rightFooter.setWidth("50%");
        footer.setWidthFull();
        Button filter = new Button(VaadinIcon.FILTER.create());
        filter.setVisible(false);//TODO Implement
        leftFooter.add(filter);
        Button add = new Button(VaadinIcon.PLUS_SQUARE_O.create());
        add.addClickListener(event -> event.getSource().getUI().ifPresent(ui -> ui.navigate(CreateBoardView.class)));
        rightFooter.add(add);
        rightFooter.setAlignItems(Alignment.END);
        rightFooter.setJustifyContentMode(JustifyContentMode.END);
        footer.add(leftFooter, rightFooter);
        add(grid, new Hr(), footer);
    }

    @Override
    public void afterNavigation(AfterNavigationEvent afterNavigationEvent) {
        grid.setItems(boardsRepository.findByPerson(this.person));
    }
}