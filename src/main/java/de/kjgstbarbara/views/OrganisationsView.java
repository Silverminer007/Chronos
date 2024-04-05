package de.kjgstbarbara.views;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParam;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.theme.lumo.LumoIcon;
import de.kjgstbarbara.data.*;
import de.kjgstbarbara.service.*;
import de.kjgstbarbara.views.nav.OrgNavigationView;
import de.kjgstbarbara.views.security.LoginView;
import jakarta.annotation.security.PermitAll;
import lombok.NonNull;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Route(value = "organisations", layout = OrgNavigationView.class)
@PermitAll
public class OrganisationsView extends Div implements BeforeEnterObserver {
    private final transient AuthenticationContext authenticationContext;
    private final OrganisationRepository organisationRepository;
    private final BoardsRepository boardsRepository;
    private final PersonsRepository personsRepository;
    private final RoleRepository roleRepository;
    private final DateRepository dateRepository;

    private final Person person;

    private final Grid<Organisation> grid = new Grid<>(Organisation.class, false);

    public OrganisationsView(OrganisationsService organisationsService, BoardsService boardsService, PersonsService personsService, RoleService roleService, DatesService datesService, AuthenticationContext authenticationContext) {
        this.organisationRepository = organisationsService.getOrganisationRepository();
        this.boardsRepository = boardsService.getBoardsRepository();
        this.personsRepository = personsService.getPersonsRepository();
        this.authenticationContext = authenticationContext;
        this.roleRepository = roleService.getRoleRepository();
        this.dateRepository = datesService.getDateRepository();


        PersonsRepository personsRepository = personsService.getPersonsRepository();
        this.person = authenticationContext.getAuthenticatedUser(UserDetails.class)
                .flatMap(userDetails -> personsRepository.findByUsername(userDetails.getUsername())).orElse(null);
        if (this.person == null) {
            authenticationContext.logout();
            throw new IllegalStateException("No user is allowed to be logged in if not in database");
        }

        HorizontalLayout header = new HorizontalLayout();
        Button newOrg = new Button("Neue Organisation...");
        newOrg.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        newOrg.addClickShortcut(Key.ENTER);
        newOrg.setTooltipText("Öffnet einen neuen Dialog um eine Organisation zu erstellen");
        newOrg.addClickListener(neueOrgEvent -> createDialog(new Organisation()));
        header.add(newOrg);
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        add(header);
        grid.addColumn("name").setHeader("Name").setAutoWidth(true);
        grid.addComponentColumn(organisation -> {
            Button edit = new Button(LumoIcon.EDIT.create());
            edit.addClickListener(editEvent -> createDialog(organisation));
            return edit;
        }).setFlexGrow(0);
        grid.addComponentColumn(organisation -> {
            Button delete = new Button(VaadinIcon.TRASH.create());
            delete.addClickListener(event -> {
                ConfirmDialog confirmDialog = new ConfirmDialog();
                confirmDialog.setHeader(organisation.getName() + " und alle zugehören Daten unwiderruflich löschen?");
                confirmDialog.setCancelable(true);
                confirmDialog.addConfirmListener(___ -> {
                    for(Date date : dateRepository.findByOrganisation(organisation)) {
                        dateRepository.delete(date);
                    }
                    for(Board board : boardsRepository.findByOrg(organisation)) {
                        for(Person p : personsRepository.findAll()) {
                            p.removeRole(Role.Type.ADMIN, Role.Scope.BOARD, board.getId());
                            p.removeRole(Role.Type.MEMBER, Role.Scope.BOARD, board.getId());
                        }
                        boardsRepository.delete(board);
                    }
                    for(Person p : personsRepository.findAll()) {
                        p.removeRole(Role.Type.ADMIN, Role.Scope.ORGANISATION, organisation.getId());
                        p.removeRole(Role.Type.MEMBER, Role.Scope.ORGANISATION, organisation.getId());
                    }
                    organisationRepository.delete(organisation);
                    updateGrid();
                });
                confirmDialog.open();
            });
            return delete;
        }).setFlexGrow(0);
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_NO_BORDER);
        updateGrid();
        grid.setSelectionMode(Grid.SelectionMode.SINGLE).addSelectionListener(selectionEvent ->
                selectionEvent.getFirstSelectedItem().ifPresent(org ->
                        selectionEvent.getSource().getUI().ifPresent(ui -> ui.navigate(BoardsView.class, new RouteParam("orgID", org.getId())))));
        add(grid);
    }

    private void createDialog(@NonNull Organisation organisation) {
        Dialog dialog = new Dialog();
        dialog.setDraggable(true);
        dialog.setHeaderTitle("Neue Organisation");
        TextField name = new TextField("Name");
        name.setRequired(true);
        Button cancel = new Button("Cancel");
        Button confirm = new Button("Confirm");
        confirm.addClickShortcut(Key.ENTER);
        confirm.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        cancel.addThemeVariants(ButtonVariant.LUMO_ERROR);
        dialog.add(name);
        dialog.getFooter().add(cancel, confirm);
        confirm.addClickListener(confirmEvent -> {
            boolean error = false;
            if (name.getValue().isBlank()) {
                name.setLabel("Der Name darf nicht leer sein");
                error = true;
            }
            if (!error) {
                organisation.setName(name.getValue());
                this.person.grantRole(Role.Type.ADMIN, Role.Scope.ORGANISATION, organisation.getId());
                this.person.saveRoles(roleRepository);
                organisationRepository.save(organisation);
                updateGrid();
                dialog.close();
            }
        });
        cancel.addClickListener(cancelEvent -> dialog.close());
        name.setAutofocus(true);
        dialog.open();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        Optional<UserDetails> optionalUserDetails = authenticationContext.getAuthenticatedUser(UserDetails.class);
        Optional<Person> optionalPerson = optionalUserDetails.flatMap(userDetails -> personsRepository.findByUsername(userDetails.getUsername()));
        if (optionalUserDetails.isEmpty() || optionalPerson.isEmpty()) {
            beforeEnterEvent.getUI().navigate(LoginView.class);
            return;
        }
        AtomicBoolean allow = new AtomicBoolean(false);
        optionalUserDetails.get().getAuthorities().forEach(grantedAuthority -> {
            if (grantedAuthority.getAuthority().equalsIgnoreCase("ADMIN")) {
                allow.set(true);
            }
        });
        if (!allow.get()) {
            beforeEnterEvent.getUI().navigate(BoardsView.class);
        }
    }

    private void updateGrid() {
        this.grid.setItems(organisationRepository.findAll());
    }
}