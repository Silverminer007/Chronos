package de.kjgstbarbara.views.createdate;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.*;
import com.vaadin.flow.spring.security.AuthenticationContext;
import de.kjgstbarbara.data.Board;
import de.kjgstbarbara.data.Date;
import de.kjgstbarbara.data.Person;
import de.kjgstbarbara.service.*;
import de.kjgstbarbara.views.DateView;
import de.kjgstbarbara.views.nav.MainNavigationView;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.function.Supplier;

@Route(value = "dates/create/:boardID", layout = MainNavigationView.class)
@PageTitle("Neuer Termin")
@PermitAll
public class CreateDateView extends VerticalLayout implements BeforeEnterObserver {
    private final PersonsRepository personsRepository;
    private final BoardsRepository boardsRepository;

    private Board board;
    private final H3 title;

    public CreateDateView(PersonsService personsService, BoardsService boardsService, DatesService datesService, AuthenticationContext authenticationContext) {
        this.personsRepository = personsService.getPersonsRepository();
        this.boardsRepository = boardsService.getBoardsRepository();
        DateRepository dateRepository = datesService.getDateRepository();
        Person person = authenticationContext.getAuthenticatedUser(UserDetails.class)
                .flatMap(userDetails -> personsRepository.findByUsername(userDetails.getUsername()))
                .orElse(null);
        if (person == null) {
            authenticationContext.logout();
        }

        this.setHeightFull();
        title = new H3();
        TextField boardTitle = new TextField("Titel des Termins");
        boardTitle.setWidthFull();
        boardTitle.setRequired(true);
        DateTimePicker startPicker = new DateTimePicker("Startzeitpunkt");
        startPicker.setRequiredIndicatorVisible(true);
        DateTimePicker endPicker = new DateTimePicker("Endzeitpunkt (optional)");
        Checkbox publish = new Checkbox("Publish to Website etc.");
        publish.setValue(false);
        publish.setWidthFull();
        Button back = new Button("Zurück");
        back.setWidthFull();
        back.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        back.addClickListener(event -> event.getSource().getUI().ifPresent(ui -> ui.navigate(DateView.class)));
        Supplier<Boolean> create = () -> {
            if (boardTitle.getValue().isBlank()) {
                boardTitle.setInvalid(true);
                boardTitle.setErrorMessage("Dieses Feld ist erforderlich");
                return false;
            } else if (startPicker.getValue() == null) {
                boardTitle.setInvalid(false);
                boardTitle.setErrorMessage("");
                startPicker.setInvalid(true);
                startPicker.setErrorMessage("Dieses Feld ist erforderlich");
                return false;
            } else {
                Date date = new Date();
                date.setBoard(board);
                date.setStart(startPicker.getValue());
                date.setEnd(endPicker.getValue());
                date.setInternal(!publish.getValue());
                date.setTitle(boardTitle.getValue());
                dateRepository.save(date);
                return true;
            }
        };
        Button save = new Button("Termin speichern");
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        save.setWidthFull();
        save.addClickShortcut(Key.ENTER);
        save.addClickListener(event -> {
            if (create.get()) {
                event.getSource().getUI().ifPresent(ui -> ui.navigate(DateView.class));
            }
        });
        VerticalLayout form = new VerticalLayout(title, boardTitle, startPicker, endPicker);
        form.setWidth(startPicker.getWidth());
        form.setHeightFull();
        VerticalLayout wrapper = new VerticalLayout(form, publish, save, back);
        wrapper.setHeightFull();
        wrapper.setJustifyContentMode(JustifyContentMode.END);
        wrapper.setWidth(startPicker.getWidth());
        this.setJustifyContentMode(JustifyContentMode.CENTER);
        this.setAlignItems(Alignment.CENTER);
        this.add(wrapper);
        this.setWidthFull();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        Board board = beforeEnterEvent.getRouteParameters().get("boardID").map(Long::valueOf).flatMap(boardsRepository::findById).orElse(null);
        if (board == null) {
            beforeEnterEvent.rerouteTo(SelectBoardView.class);
        } else {
            this.board = board;
            this.title.setText("Neuer Termin für: " + this.board.getTitle());
        }
    }
}