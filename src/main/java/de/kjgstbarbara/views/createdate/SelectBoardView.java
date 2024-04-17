package de.kjgstbarbara.views.createdate;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParam;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.spring.security.AuthenticationContext;
import de.kjgstbarbara.data.Board;
import de.kjgstbarbara.data.Person;
import de.kjgstbarbara.service.BoardsRepository;
import de.kjgstbarbara.service.BoardsService;
import de.kjgstbarbara.service.PersonsRepository;
import de.kjgstbarbara.service.PersonsService;
import de.kjgstbarbara.views.CreateBoardView;
import de.kjgstbarbara.views.DateView;
import de.kjgstbarbara.views.nav.MainNavigationView;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Route(value = "dates/select-board", layout = MainNavigationView.class)
@PageTitle("Board auswählen")
@PermitAll
public class SelectBoardView extends VerticalLayout {
    private final PersonsRepository personsRepository;

    public SelectBoardView(PersonsService personsService, BoardsService boardsService, AuthenticationContext authenticationContext) {
        this.personsRepository = personsService.getPersonsRepository();
        BoardsRepository boardsRepository = boardsService.getBoardsRepository();
        Person person = authenticationContext.getAuthenticatedUser(UserDetails.class)
                .flatMap(userDetails -> personsRepository.findByUsername(userDetails.getUsername()))
                .orElse(null);
        if (person == null) {
            authenticationContext.logout();
        }

        this.setHeightFull();
        H2 title = new H2("In welchem Board soll ein neuer Termin erstellt werden?");
        Grid<Board> selectBoard = new Grid<>();
        selectBoard.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_ROW_STRIPES);
        Button createBoard = new Button("Neues Board", VaadinIcon.PLUS.create());
        createBoard.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        createBoard.addClickListener(event -> event.getSource().getUI().ifPresent(ui -> ui.navigate(CreateBoardView.class)));
        selectBoard.addColumn(Board::getTitle).setHeader("Boards").setFooter(createBoard);
        selectBoard.setItems(boardsRepository.findByAdmin(person));
        selectBoard.setSelectionMode(Grid.SelectionMode.SINGLE);
        Button back = new Button("Zurück");
        back.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        back.addClickListener(event -> event.getSource().getUI().ifPresent(ui -> ui.navigate(DateView.class)));
        Button cont = new Button("Weiter");
        cont.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        cont.addClickShortcut(Key.ENTER);
        cont.addClickListener(event -> {
            Optional<Board> selectedBoard = selectBoard.getSelectedItems().stream().findFirst();
            if(selectedBoard.isPresent()) {
                event.getSource().getUI().ifPresent(ui -> ui.navigate(CreateDateView.class, new RouteParameters(new RouteParam("boardID", selectedBoard.get().getId()))));
            } else {
                Notification.show("Bitte wähle zuerst ein Board aus").addThemeVariants(NotificationVariant.LUMO_WARNING);
            }
        });
        VerticalLayout form = new VerticalLayout(title, selectBoard);
        form.setHeightFull();
        HorizontalLayout navigation = new HorizontalLayout(back, cont);
        navigation.setWidthFull();
        navigation.setJustifyContentMode(JustifyContentMode.BETWEEN);
        VerticalLayout wrapper = new VerticalLayout(form, navigation);
        wrapper.setHeightFull();
        wrapper.setJustifyContentMode(JustifyContentMode.END);
        this.setJustifyContentMode(JustifyContentMode.CENTER);
        this.setAlignItems(Alignment.CENTER);
        this.add(wrapper);

    }
}