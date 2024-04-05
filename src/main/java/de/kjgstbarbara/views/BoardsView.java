package de.kjgstbarbara.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.router.*;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.theme.lumo.LumoIcon;
import de.kjgstbarbara.data.Board;
import de.kjgstbarbara.data.Organisation;
import de.kjgstbarbara.data.Person;
import de.kjgstbarbara.data.Role;
import de.kjgstbarbara.service.*;
import de.kjgstbarbara.views.modules.MembersView;
import de.kjgstbarbara.views.nav.BoardsNavigationView;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.List;

@Route(value = "boards/:orgID?", layout = BoardsNavigationView.class)
@PermitAll
public class BoardsView extends Div implements BeforeEnterObserver, AfterNavigationObserver {
    private final BoardsRepository boardsRepository;
    private final RoleRepository roleRepository;
    private final OrganisationRepository organisationRepository;
    private final List<Organisation> organisations = new ArrayList<>();
    private final Person person;
    private final Select<Organisation> organisationSelect = new Select<>();
    private final H1 title = new H1();
    private final Grid<Board> grid = new Grid<>(Board.class, false);
    private final Button newBoard = new Button("Neues Board...");

    public BoardsView(PersonsService personsService, BoardsService boardsService, RoleService roleService, OrganisationsService organisationsService, AuthenticationContext authenticationContext) {
        this.boardsRepository = boardsService.getBoardsRepository();
        this.roleRepository = roleService.getRoleRepository();
        this.organisationRepository = organisationsService.getOrganisationRepository();
        PersonsRepository personsRepository = personsService.getPersonsRepository();
        this.person = authenticationContext.getAuthenticatedUser(UserDetails.class)
                .flatMap(userDetails -> personsRepository.findByUsername(userDetails.getUsername())).orElse(null);
        if (this.person == null) {
            authenticationContext.logout();
            throw new IllegalStateException("No user is allowed to be logged in if not in database");
        }

        HorizontalLayout header = new HorizontalLayout();
        newBoard.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        newBoard.addClickShortcut(Key.ENTER);
        newBoard.addClickListener(neueOrgEvent -> createDialog(new Board(), "Neues Board").open());
        newBoard.setEnabled(true);
        header.add(newBoard);
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        add(header);
        grid.addColumn("title").setHeader("Titel").setAutoWidth(true);
        grid.addComponentColumn(board -> {
            Button edit = new Button(LumoIcon.EDIT.create());
            edit.addClickListener(editEvent -> createDialog(board, "Board bearbeiten").open());
            edit.setEnabled(this.person.hasAuthority(Role.Type.ADMIN, Role.Scope.BOARD, board.getId()));
            return edit;
        }).setFlexGrow(0);
        grid.addComponentColumn(board -> {
            Button delete = new Button(VaadinIcon.TRASH.create());
            delete.addClickListener(event -> {
                ConfirmDialog confirmDialog = new ConfirmDialog();
                confirmDialog.setHeader("Delete " + board.getTitle() + "?");
                confirmDialog.setCancelable(true);
                confirmDialog.addConfirmListener(___ -> {
                    boardsRepository.deleteById(board.getId());
                    updateGrid();
                });
                confirmDialog.open();
            });
            delete.setEnabled(this.person.hasAuthority(Role.Type.ADMIN, Role.Scope.BOARD, board.getId()));
            return delete;
        }).setFlexGrow(0);
        add(grid);
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_NO_BORDER);

        grid.setSelectionMode(Grid.SelectionMode.SINGLE).addSelectionListener(event ->
                event.getSource().getUI().ifPresent(ui ->
                        event.getFirstSelectedItem().ifPresent(item ->
                                ui.navigate(MembersView.class,
                                        new RouteParam("boardID", item.getId())))));
    }

    private void updateGrid() {
        List<Board> boards = new ArrayList<>();
        for (Organisation org : organisations) {
            boards.addAll(boardsRepository.findByOrg(org));
        }
        grid.setItems(boards);
    }

    private Dialog createDialog(Board board, String dialogTitle) {
        Dialog dialog = new Dialog();
        Binder<Board> binder = new Binder<>();
        dialog.setDraggable(true);
        dialog.setHeaderTitle(dialogTitle);
        TextField title = new TextField("Title");
        title.setAutofocus(true);
        title.setRequired(true);
        binder.forField(title).bind(Board::getTitle, Board::setTitle);
        Select<Organisation> organisationSelect = new Select<>();
        List<Organisation> orgsPersonIsAdmin = organisationRepository.hasAuthorityOn(Role.Type.ADMIN, this.person);
        organisationSelect.setVisible(board.getOrganisation() == null);
        organisationSelect.setItems(orgsPersonIsAdmin);
        organisationSelect.setRequiredIndicatorVisible(true);
        organisationSelect.setLabel("Organisation");

        Button cancel = new Button("Cancel");
        Button confirm = new Button("Confirm");
        confirm.addClickShortcut(Key.ENTER);
        confirm.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        cancel.addThemeVariants(ButtonVariant.LUMO_ERROR);
        VerticalLayout verticalLayout = new VerticalLayout(title, organisationSelect);
        dialog.add(verticalLayout);
        dialog.getFooter().add(cancel, confirm);
        confirm.addClickListener(confirmEvent -> {
            if (title.getValue().isBlank()) {
                title.setLabel("Der Titel darf nicht leer sein");
            } else {
                try {
                    binder.writeBean(board);
                    if (organisationSelect.getValue() == null && board.getOrganisation() != null) {
                        organisationSelect.setInvalid(true);
                        organisationSelect.setErrorMessage("Es ist eine Auswahl Erforderlich");
                    } else {
                        organisationSelect.setInvalid(false);
                        organisationSelect.setErrorMessage("");
                        if (board.getOrganisation() == null) {
                            board.setOrganisation(organisationSelect.getValue());
                        }
                        boardsRepository.save(board);
                        updateGrid();
                        dialog.close();
                    }
                } catch (ValidationException e) {
                    Notification.show(e.getLocalizedMessage());
                }
            }
        });
        confirm.setEnabled(false);
        organisationSelect.addValueChangeListener(selectOrganisationComponentValueChangeEvent -> {
            if (selectOrganisationComponentValueChangeEvent.getValue() != null)
                confirm.setEnabled(true);
        });
        binder.readBean(board);
        cancel.addClickListener(cancelEvent -> dialog.close());
        return dialog;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        Organisation org = beforeEnterEvent.getRouteParameters().get("orgID").map(Long::valueOf).flatMap(organisationRepository::findById).orElse(null);
        organisations.clear();
        if (org == null) {
            organisations.addAll(organisationRepository.findAll().stream().filter(o ->
                    this.person.hasAuthority(Role.Type.MEMBER, Role.Scope.ORGANISATION, o.getId())).toList());
            title.setText("Alle Boards");
        } else {
            organisations.add(org);
            title.setText("Boards der " + org);
        }
    }

    @Override
    public void afterNavigation(AfterNavigationEvent afterNavigationEvent) {
        organisationSelect.setItems(organisationRepository.hasAuthorityOn(Role.Type.MEMBER, this.person));
        organisationSelect.addValueChangeListener(selectOrganisationComponentValueChangeEvent ->
                selectOrganisationComponentValueChangeEvent.getSource().getUI().ifPresent(ui -> {
                    if (selectOrganisationComponentValueChangeEvent.getValue() != null) {
                        ui.navigate(BoardsView.class, new RouteParameters(new RouteParam("orgID", selectOrganisationComponentValueChangeEvent.getValue().getId())));
                        title.setText("Boards der " + selectOrganisationComponentValueChangeEvent.getValue().toString());
                    }
                }));
        newBoard.setEnabled(organisations.stream().anyMatch(org -> this.person.hasAuthority(Role.Type.ADMIN, Role.Scope.ORGANISATION, org.getId())));
        newBoard.setTooltipText(newBoard.isEnabled() ? "Wähle eine Organisation für die du ein neues Board erstellst"
                : organisations.isEmpty() ? "Es gibt keine Organisation" : "Du hast keine Berechtigung ein neues Board anzulegen");
        updateGrid();
    }

    public Component header() {
        return new HorizontalLayout(title, organisationSelect);
    }
}