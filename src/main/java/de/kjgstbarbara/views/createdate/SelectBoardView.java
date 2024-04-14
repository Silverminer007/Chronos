package de.kjgstbarbara.views.createdate;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
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
import de.kjgstbarbara.views.BoardView;
import de.kjgstbarbara.views.CreateBoardView;
import de.kjgstbarbara.views.DateView;
import de.kjgstbarbara.views.nav.MainNavigationView;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.function.Supplier;

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
        Select<Board> selectBoard = new Select<>();
        selectBoard.setLabel("In welchem Board soll ein neuer Termin erstellt werden?");
        selectBoard.setItems(boardsRepository.findByAdmin(person));
        selectBoard.setWidthFull();
        Button createBoard = new Button("Zuerst ein Board erstellen");
        createBoard.setWidthFull();
        createBoard.addClickListener(event -> event.getSource().getUI().ifPresent(ui -> ui.navigate(CreateBoardView.class)));
        Button back = new Button("Zurück");
        back.setWidthFull();
        back.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        back.addClickListener(event -> event.getSource().getUI().ifPresent(ui -> ui.navigate(DateView.class)));
        Button confirmNClose = new Button("Weiter");
        confirmNClose.setWidthFull();
        confirmNClose.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        confirmNClose.addClickShortcut(Key.ENTER);
        Supplier<Boolean> create = () -> {
            if (selectBoard.getValue() == null) {
                selectBoard.setInvalid(true);
                selectBoard.setErrorMessage("Dieses Feld ist erforderlich");
                return false;
            } else {
                return true;
            }
        };
        confirmNClose.addClickListener(event -> {
            if (create.get()) {
                event.getSource().getUI().ifPresent(ui -> ui.navigate(CreateDateView.class, new RouteParameters(new RouteParam("boardID", selectBoard.getValue().getId()))));
            }
        });
        VerticalLayout form = new VerticalLayout(selectBoard, createBoard);
        form.setHeightFull();
        VerticalLayout wrapper = new VerticalLayout(form, confirmNClose, back);
        wrapper.setHeightFull();
        wrapper.setJustifyContentMode(JustifyContentMode.END);
        this.setJustifyContentMode(JustifyContentMode.CENTER);
        this.setAlignItems(Alignment.CENTER);
        this.add(wrapper);

    }
}