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
import de.kjgstbarbara.FriendlyError;
import de.kjgstbarbara.data.Board;
import de.kjgstbarbara.data.Person;
import de.kjgstbarbara.data.Role;
import de.kjgstbarbara.service.*;
import de.kjgstbarbara.views.BoardsView;
import de.kjgstbarbara.views.components.LongNumberField;
import de.kjgstbarbara.views.nav.ModuleNavigationView;
import de.kjgstbarbara.whatsapp.WhatsAppUtils;
import jakarta.annotation.security.PermitAll;
import lombok.NonNull;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.List;

@Route(value = "board/:boardID/members", layout = ModuleNavigationView.class)
@RouteAlias(value = "board/:boardID", layout = ModuleNavigationView.class)
@PermitAll
public class MembersView extends VerticalLayout implements BeforeEnterObserver, AfterNavigationObserver {
    private final BoardsRepository boardsRepository;
    private final PersonsRepository personsRepository;
    private final RoleRepository roleRepository;
    private final PasswordResetRepository passwordResetRepository;

    private final Person person;

    private Board board;
    private final Grid<Person> grid = new Grid<>(Person.class, false);

    public MembersView(BoardsService boardsService, PersonsService personsService, RoleService roleService, PasswordResetService passwordResetService, AuthenticationContext authenticationContext) {
        this.boardsRepository = boardsService.getBoardsRepository();
        this.personsRepository = personsService.getPersonsRepository();
        this.roleRepository = roleService.getRoleRepository();
        this.passwordResetRepository = passwordResetService.getPasswordResetRepository();

        PersonsRepository personsRepository = personsService.getPersonsRepository();
        this.person = authenticationContext.getAuthenticatedUser(UserDetails.class)
                .flatMap(userDetails -> personsRepository.findByUsername(userDetails.getUsername())).orElse(null);
        if (this.person == null) {
            authenticationContext.logout();
            throw new IllegalStateException("No user is allowed to be logged in if not in database");
        }

        grid.addColumn("firstName").setHeader("Vorname")
                .setAutoWidth(true).setResizable(true);
        grid.addColumn("lastName").setHeader("Nachname").setAutoWidth(true).setResizable(true);
        grid.addColumn("birthDate").setHeader("Geburtsdatum").setAutoWidth(true).setResizable(true);
        grid.addColumn(person -> "+" + person.getPhoneNumber()).setHeader("Telefonnummer").setAutoWidth(true).setResizable(true);
        grid.addComponentColumn(person ->
                createStatusIcon(person.hasRole(Role.Type.ADMIN, Role.Scope.BOARD, board.getId()))).setHeader("Board Admin");
        grid.addComponentColumn(person ->
                createStatusIcon(!person.getUsername().isBlank())).setHeader("Aktiv");
        grid.addComponentColumn(person -> {
            Button edit = new Button(LumoIcon.EDIT.create());
            edit.addClickListener(event -> editMemberDialog(person, "Mitglied bearbeiten", true));
            return edit;
        }).setFlexGrow(0);
        grid.addComponentColumn(deletedPerson -> {
            Button delete = new Button(VaadinIcon.TRASH.create());
            delete.addClickListener(event -> {
                        ConfirmDialog confirmDialog = new ConfirmDialog();
                        confirmDialog.setHeader(deletedPerson.getFirstName() + " " + deletedPerson.getLastName() + " löschen?");
                        confirmDialog.setCancelable(true);
                        confirmDialog.addConfirmListener(___ -> {
                            deletedPerson.removeRole(Role.Type.MEMBER, Role.Scope.BOARD, this.board.getId());
                            deletedPerson.removeRole(Role.Type.ADMIN, Role.Scope.BOARD, this.board.getId());
                            if (this.person.hasAuthority(Role.Type.ADMIN, Role.Scope.ORGANISATION, null)
                                    && boardsRepository.hasAuthorityOn(Role.Type.MEMBER, deletedPerson).isEmpty()) {
                                ConfirmDialog confirmDialog1 = new ConfirmDialog();
                                confirmDialog1.setHeader("Soll " + deletedPerson.getFirstName() + " " + deletedPerson.getLastName() + " unwiderruflich gelöscht werden?");
                                confirmDialog1.setCancelButton("Nein", ____ -> {
                                });
                                confirmDialog1.setConfirmButton("Ja", ____ -> personsRepository.delete(deletedPerson));
                                confirmDialog1.open();
                            }
                            boardsRepository.save(board);
                            personsRepository.save(deletedPerson);
                            updateGrid();
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
        board = beforeEnterEvent.getRouteParameters().get("boardID").map(Long::valueOf).flatMap(boardsRepository::findById).orElse(null);
        if (board == null) {
            beforeEnterEvent.forwardTo(BoardsView.class);
        }
    }

    private void editMemberDialog(@NonNull Person member, String title, boolean edit) {
        Dialog dialog = new Dialog(title);

        Binder<Person> binder = new Binder<>();

        VerticalLayout verticalLayout = new VerticalLayout();

        List<Person> existingPersons = new ArrayList<>(
                this.person.hasAuthority(Role.Type.ADMIN, Role.Scope.ALL, null) ?
                        personsRepository.findAll() :
                        personsRepository.hasAuthorityOn(Role.Type.MEMBER, board.getOrganisation()));
        existingPersons.removeAll(personsRepository.hasRoleOn(Role.Type.MEMBER, this.board));
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
                    addMember(board, p, existingAdmin.getValue());
                    personsRepository.save(p);
                }
                boardsRepository.save(board);
                updateGrid();
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
        checkbox.setValue(member.hasRole(Role.Type.ADMIN, Role.Scope.BOARD, board.getId()));
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
                addMember(board, member, checkbox.getValue());
                personsRepository.save(member);
                boardsRepository.save(board);
                passwordResetRepository.save(WhatsAppUtils.getInstance().sendPasswordResetMessage(member, WhatsAppUtils.CREATED));
                updateGrid();
                dialog.close();
            } catch (ValidationException e) {
                for (ValidationResult result : e.getValidationErrors()) {
                    Notification.show(result.getErrorMessage());
                }
            } catch (FriendlyError e) {
                Notification.show(e.getMessage());
            }
        });
        dialog.getFooter().add();

        binder.readBean(member);
        dialog.open();
    }

    private void addMember(Board board, Person person, boolean admin) {
        person.grantRole(Role.Type.MEMBER, Role.Scope.BOARD, board.getId());
        if (admin) {
            person.grantRole(Role.Type.ADMIN, Role.Scope.BOARD, board.getId());
        } else {
            person.removeRole(Role.Type.ADMIN, Role.Scope.BOARD, board.getId());
        }
        person.grantRole(Role.Type.MEMBER, Role.Scope.ORGANISATION, board.getOrganisation().getId());
        person.saveRoles(roleRepository);
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
        grid.setItems(personsRepository.hasRoleOn(Role.Type.MEMBER, this.board));
    }
}