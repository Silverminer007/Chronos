package de.kjgstbarbara.views;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.router.*;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.theme.lumo.LumoIcon;
import de.kjgstbarbara.data.Board;
import de.kjgstbarbara.data.Organisation;
import de.kjgstbarbara.data.Role;
import de.kjgstbarbara.service.*;
import de.kjgstbarbara.views.modules.MembersView;
import de.kjgstbarbara.views.nav.BoardsNavigationView;
import de.kjgstbarbara.views.nav.OrgNavigationView;
import jakarta.annotation.security.PermitAll;
import lombok.NonNull;

@Route(value = "org/:orgID", layout = BoardsNavigationView.class)
@PermitAll
public class BoardsView extends Div implements BeforeEnterObserver, AfterNavigationObserver {
    private final BoardsRepository boardsRepository;
    private final OrganisationRepository organisationRepository;
    private final RoleRepository roleRepository;
    private Organisation organisation;
    private final H2 title = new H2("");
    private final Grid<Board> grid = new Grid<>(Board.class, false);

    public BoardsView(OrganisationsService organisationsService, BoardsService boardsService, RoleService roleService) {
        this.boardsRepository = boardsService.getBoardsRepository();
        this.organisationRepository = organisationsService.getOrganisationRepository();
        this.roleRepository = roleService.getRoleRepository();

        HorizontalLayout header = new HorizontalLayout();
        Button newBoard = new Button("Neues Board...");
        newBoard.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        newBoard.addClickShortcut(Key.ENTER);
        newBoard.addClickListener(neueOrgEvent -> createDialog(new Board(), "Neues Board").open());
        header.add(newBoard);
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        add(header);
        grid.addColumn("title").setHeader("Titel").setAutoWidth(true);
        grid.addComponentColumn(board -> {
            Button edit = new Button(LumoIcon.EDIT.create());
            edit.addClickListener(editEvent -> createDialog(board, "Board bearbeiten").open());
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
                    grid.setItems(boardsRepository.findByOrg(organisation));
                });
                confirmDialog.open();
            });
            return delete;
        }).setFlexGrow(0);
        add(grid);
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_NO_BORDER);

        grid.setSelectionMode(Grid.SelectionMode.SINGLE).addSelectionListener(event -> {
            event.getSource().getUI().ifPresent(ui ->
                    event.getFirstSelectedItem().ifPresent(item ->
                            ui.navigate(MembersView.class,
                                    new RouteParam("orgID", organisation.getId()),
                                    new RouteParam("boardID", item.getId()))));
        });
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
        Button cancel = new Button("Cancel");
        Button confirm = new Button("Confirm");
        confirm.addClickShortcut(Key.ENTER);
        confirm.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        cancel.addThemeVariants(ButtonVariant.LUMO_ERROR);
        dialog.add(title);
        dialog.getFooter().add(cancel, confirm);
        confirm.addClickListener(confirmEvent -> {
            if (title.getValue().isBlank()) {
                title.setLabel("Der Titel darf nicht leer sein");
            } else {
                try {
                    binder.writeBean(board);
                    Role requiredRole = board.getRequiredRole() == null ? createRole(board) : board.getRequiredRole();
                    board.setRequiredRole(requiredRole);
                    roleRepository.save(requiredRole);
                    board.setOrganisation(organisation);
                    boardsRepository.save(board);
                    grid.setItems(boardsRepository.findByOrg(organisation));
                    dialog.close();
                } catch (ValidationException e) {
                    Notification.show(e.getLocalizedMessage());
                }
            }
        });
        cancel.addClickListener(cancelEvent -> dialog.close());
        return dialog;
    }

    private @NonNull Role createRole(Board board) {
        Role r = new Role();
        r.setRole("BOARD_ADMIN_" + board.getId());
        return r;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        organisation = beforeEnterEvent.getRouteParameters().get("orgID").flatMap(organisationRepository::findById).orElse(null);
        if (organisation == null) {
            beforeEnterEvent.rerouteTo(OrgNavigationView.class);
        }
    }

    @Override
    public void afterNavigation(AfterNavigationEvent afterNavigationEvent) {
        title.setText(organisation.getName());
        grid.setItems(boardsRepository.findByOrg(organisation));
    }
}