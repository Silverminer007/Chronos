package de.kjgstbarbara.views.modules;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.listbox.MultiSelectListBox;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.router.*;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.theme.lumo.LumoIcon;
import de.kjgstbarbara.data.Board;
import de.kjgstbarbara.data.Organisation;
import de.kjgstbarbara.data.Person;
import de.kjgstbarbara.service.*;
import de.kjgstbarbara.views.BoardsView;
import de.kjgstbarbara.views.NotFoundView;
import de.kjgstbarbara.views.OrganisationsView;
import de.kjgstbarbara.views.components.LongNumberField;
import de.kjgstbarbara.views.nav.ModuleNavigationView;
import jakarta.annotation.security.PermitAll;
import lombok.NonNull;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Route(value = "org/:orgID/board/:boardID/members", layout = ModuleNavigationView.class)
@RouteAlias(value = "org/:orgID/board/:boardID")
@PermitAll
public class MembersView extends VerticalLayout implements BeforeEnterObserver, AfterNavigationObserver {
    private final transient AuthenticationContext authenticationContext;
    private final OrganisationRepository organisationRepository;
    private final BoardsRepository boardsRepository;
    private final PersonsRepository personsRepository;

    private Board board;
    private Organisation organisation;
    private final Grid<Person> grid = new Grid<>(Person.class, false);

    public MembersView(OrganisationsService organisationsService, BoardsService boardsService, PersonsService personsService, AuthenticationContext authenticationContext) {
        this.organisationRepository = organisationsService.getOrganisationRepository();
        this.boardsRepository = boardsService.getBoardsRepository();
        this.personsRepository = personsService.getPersonsRepository();
        this.authenticationContext = authenticationContext;

        grid.addColumn("firstName").setHeader("Vorname")
                .setAutoWidth(true).setResizable(true);
        grid.addColumn("lastName").setHeader("Nachname").setAutoWidth(true).setResizable(true);
        grid.addColumn("birthDate").setHeader("Geburtsdatum").setAutoWidth(true).setResizable(true);
        grid.addColumn(person -> "+" + person.getPhoneNumber()).setHeader("Telefonnummer").setAutoWidth(true).setResizable(true);
        grid.addComponentColumn(person ->
                createStatusIcon(board.isAdmin(person))).setHeader("Board Admin");
        grid.addComponentColumn(person -> {
            Button edit = new Button(LumoIcon.EDIT.create());
            edit.addClickListener(event -> editMemberDialog(person, "Mitglied bearbeiten", true));
            return edit;
        }).setFlexGrow(0);
        grid.addComponentColumn(person -> {
            Button delete = new Button(VaadinIcon.TRASH.create());
            delete.addClickListener(event -> {
                        ConfirmDialog confirmDialog = new ConfirmDialog();
                        confirmDialog.setHeader(person.getFirstName() + " " + person.getLastName() + " löschen?");
                        confirmDialog.setCancelable(true);
                        confirmDialog.addConfirmListener(___ -> {
                            board.getMembers().remove(person);
                            boardsRepository.save(board);
                            grid.setItems(board.getMembers());
                        });
                        confirmDialog.open();
                    }
            );
            return delete;
        }).setFlexGrow(0);
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_ROW_STRIPES);

        Button button = new Button("Neues Mitglied");
        button.addClickListener(event ->
                editMemberDialog(new Person(), "Neues Mitglied erstellen", false));
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
        organisation = beforeEnterEvent.getRouteParameters().get("orgID").flatMap(organisationRepository::findById).orElse(null);
        /*organisation = authenticationContext.getAuthenticatedUser(UserDetails.class)
                .flatMap(userDetails -> personsRepository.findByUsername(userDetails.getUsername()))
                .map(Person::getOrganisation)
                .orElse(null);*/
        board = beforeEnterEvent.getRouteParameters().get("boardID").map(Long::valueOf).flatMap(boardsRepository::findById).orElse(null);
        if (organisation == null) {
            beforeEnterEvent.forwardTo(OrganisationsView.class);
        } else if (board == null) {
            beforeEnterEvent.forwardTo(BoardsView.class, new RouteParameters("orgID", organisation.getId()));
        }
    }

    private void editMemberDialog(@NonNull Person member, String title, boolean edit) {
        Dialog dialog = new Dialog(title);

        Binder<Person> binder = new Binder<>();

        VerticalLayout verticalLayout = new VerticalLayout();

        List<Person> existingPersons = new ArrayList<>(personsRepository.findByOrg(organisation));
        existingPersons.removeAll(board.getMembers());
        if (!edit && !existingPersons.isEmpty()) {
            verticalLayout.add(new H3("Bestehendes Mitglied zu Board hinzufügen"));
            MultiSelectListBox<Person> select = new MultiSelectListBox<>();
            select.setItems(existingPersons);
            verticalLayout.add(select);

            Checkbox existingAdmin = new Checkbox("Alle zum Admin ernennen");
            existingAdmin.setValue(false);
            verticalLayout.add(existingAdmin);

            Button addExisting = new Button("Hinzufügen");
            addExisting.addClickListener(event -> {
                for (Person p : select.getValue()) {
                    if (!board.getMembers().contains(p)) {
                        board.getMembers().add(p);
                    }
                    if (existingAdmin.getValue()) {
                        if (!p.getRoles().contains(board.getRequiredRole())) {
                            p.getRoles().add(board.getRequiredRole());
                        }
                    } else {
                        p.getRoles().remove(board.getRequiredRole());
                    }
                }
                boardsRepository.save(board);
                grid.setItems(board.getMembers());
                dialog.close();
            });
            verticalLayout.add(addExisting);
            verticalLayout.add(new Hr());
            verticalLayout.add(new H3("Neues Mitglied erstellen"));
        }


        HorizontalLayout nameLayout = new HorizontalLayout();
        TextField firstName = new TextField("Vorname");
        firstName.setAutofocus(existingPersons.isEmpty());
        binder.forField(firstName).bind(Person::getFirstName, Person::setFirstName);
        TextField lastName = new TextField("Nachname");
        binder.forField(lastName).bind(Person::getLastName, Person::setLastName);
        nameLayout.add(firstName, lastName);
        verticalLayout.add(nameLayout);

        HorizontalLayout line2 = new HorizontalLayout();
        DatePicker datePicker = new DatePicker("Geburtsdatum");
        binder.forField(datePicker).bind(Person::getBirthDate, Person::setBirthDate);
        line2.add(datePicker);

        LongNumberField numberField = new LongNumberField("Telefonnummer");
        binder.forField(numberField)
                .bind(Person::getPhoneNumber, Person::setPhoneNumber);
        line2.add(numberField);

        verticalLayout.add(line2);

        Checkbox checkbox = new Checkbox("Board Admin");
        checkbox.setValue(board.isAdmin(member));
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
                binder.writeBean(member);
                member.setOrganisation(organisation);
                personsRepository.save(member);
                if (!board.getMembers().contains(member)) {
                    board.getMembers().add(member);
                }
                if (checkbox.getValue()) {
                    if (!member.getRoles().contains(board.getRequiredRole())) {
                        member.getRoles().add(board.getRequiredRole());
                    }
                } else {
                    member.getRoles().remove(board.getRequiredRole());
                }
                boardsRepository.save(board);
                grid.setItems(board.getMembers());
                dialog.close();
            } catch (ValidationException e) {
                for (ValidationResult result : e.getValidationErrors()) {
                    Notification.show(result.getErrorMessage());
                }
            }
        });
        dialog.getFooter().add();

        binder.readBean(member);
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
        grid.setItems(board.getMembers());
    }
}