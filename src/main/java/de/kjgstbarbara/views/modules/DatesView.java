package de.kjgstbarbara.views.modules;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.renderer.LocalDateTimeRenderer;
import com.vaadin.flow.router.*;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.theme.lumo.LumoIcon;
import de.kjgstbarbara.data.Board;
import de.kjgstbarbara.data.Date;
import de.kjgstbarbara.data.Person;
import de.kjgstbarbara.data.Role;
import de.kjgstbarbara.service.*;
import de.kjgstbarbara.views.BoardsView;
import de.kjgstbarbara.views.nav.ModuleNavigationView;
import jakarta.annotation.security.PermitAll;
import lombok.NonNull;
import org.springframework.security.core.userdetails.UserDetails;

@Route(value = "board/:boardID/dates", layout = ModuleNavigationView.class)
@PermitAll
public class DatesView extends VerticalLayout implements BeforeEnterObserver, AfterNavigationObserver {
    private final BoardsRepository boardsRepository;
    private final DateRepository dateRepository;

    private final Person person;

    private Board board;
    private final Grid<Date> grid = new Grid<>(Date.class, false);

    public DatesView(BoardsService boardsService, DatesService datesService, PersonsService personsService, AuthenticationContext authenticationContext) {
        this.boardsRepository = boardsService.getBoardsRepository();
        this.dateRepository = datesService.getDateRepository();

        PersonsRepository personsRepository = personsService.getPersonsRepository();
        this.person = authenticationContext.getAuthenticatedUser(UserDetails.class)
                .flatMap(userDetails -> personsRepository.findByUsername(userDetails.getUsername())).orElse(null);
        if (this.person == null) {
            authenticationContext.logout();
            throw new IllegalStateException("No user is allowed to be logged in if not in database");
        }

        grid.addColumn("title").setHeader("Termin")
                .setAutoWidth(true).setResizable(true);
        grid.addColumn("start")
                .setRenderer(new LocalDateTimeRenderer<>(Date::getStart, "dd.MM.YYYY HH:mm"))
                .setHeader("Datum").setAutoWidth(true).setResizable(true);
        grid.addComponentColumn(person ->
                createStatusIcon(person.isInternal())).setHeader("Intern").setFlexGrow(0);
        grid.addComponentColumn(date -> {
            Button edit = new Button(LumoIcon.EDIT.create());
            edit.addClickListener(event -> editDialog(date, "Termin bearbeiten"));
            return edit;
        }).setFlexGrow(0);
        grid.addComponentColumn(date -> {
            Button delete = new Button(VaadinIcon.TRASH.create());
            delete.addClickListener(event -> {
                        ConfirmDialog confirmDialog = new ConfirmDialog();
                        confirmDialog.setHeader(date.getTitle() + " am " + date.getStart() + " löschen?");
                        confirmDialog.setCancelable(true);
                        confirmDialog.addConfirmListener(___ -> {
                            dateRepository.delete(date);
                            updateGrid();
                        });
                        confirmDialog.open();
                    }
            );
            return delete;
        }).setFlexGrow(0);
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_ROW_STRIPES);

        Button button = new Button("Neuer Termin");
        button.addClickListener(event ->
                editDialog(new Date(), "Neuen Termin erstellen"));
        button.addClickShortcut(Key.ENTER);
        button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        HorizontalLayout horizontalLayout = new HorizontalLayout(button);
        horizontalLayout.setJustifyContentMode(JustifyContentMode.END);
        horizontalLayout.setWidthFull();

        add(horizontalLayout);

        add(grid);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        board = beforeEnterEvent.getRouteParameters().get("boardID").map(Long::valueOf).flatMap(boardsRepository::findById).orElse(null);
        if (board == null) {
            beforeEnterEvent.forwardTo(BoardsView.class);
        }
    }

    private void editDialog(@NonNull Date date, String dialogTitle) {
        Dialog dialog = new Dialog(dialogTitle);

        Binder<Date> binder = new Binder<>();

        VerticalLayout verticalLayout = new VerticalLayout();

        TextField title = new TextField("Titel");
        title.setAutofocus(true);
        title.setWidthFull();
        binder.forField(title).bind(Date::getTitle, Date::setTitle);
        verticalLayout.add(title);

        DateTimePicker datePicker = new DateTimePicker("Startzeitpunkt");
        binder.forField(datePicker).bind(Date::getStart, Date::setStart);
        verticalLayout.add(datePicker);

        Checkbox checkbox = new Checkbox("Intern");
        binder.forField(checkbox).bind(Date::isInternal, Date::setInternal);
        verticalLayout.add(checkbox);

        dialog.add(verticalLayout);

        Button cancelButton = new Button("Cancel");
        cancelButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
        Button confirmButton = new Button("Confirm");
        confirmButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        confirmButton.addClickShortcut(Key.ENTER);
        dialog.getFooter().add(cancelButton, confirmButton);
        cancelButton.addClickListener(event -> dialog.close());
        confirmButton.addClickListener(event -> {
            try {
                binder.writeBean(date);
                date.setBoard(this.board);
                dateRepository.save(date);
                boardsRepository.save(board);
                updateGrid();
                dialog.close();
            } catch (ValidationException e) {
                for (ValidationResult result : e.getValidationErrors()) {
                    Notification.show(result.getErrorMessage());
                }
            }
        });
        dialog.getFooter().add();

        binder.readBean(date);
        dialog.open();
    }

    private Icon createStatusIcon(boolean status) {
        Icon icon;
        if (status) {
            icon = VaadinIcon.CHECK.create();
            icon.getElement().getThemeList().add("badge success");
        } else {
            icon = VaadinIcon.CLOSE_SMALL.create();
            icon.getElement().getThemeList().add("badge error");
        }
        icon.getStyle().set("padding", "var(--lumo-space-xs");
        return icon;
    }

    @Override
    public void afterNavigation(AfterNavigationEvent afterNavigationEvent) {
        updateGrid();
    }

    private void updateGrid() {
        grid.setItems(dateRepository.hasAuthorityOn(Role.Type.MEMBER, this.person).stream().filter(date -> date.getBoard().equals(this.board)).toList());
    }
}