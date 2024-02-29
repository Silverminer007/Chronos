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
import com.vaadin.flow.router.*;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.theme.lumo.LumoIcon;
import de.kjgstbarbara.data.Organisation;
import de.kjgstbarbara.data.Person;
import de.kjgstbarbara.service.*;
import de.kjgstbarbara.views.nav.OrgNavigationView;
import jakarta.annotation.Nullable;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;

@Route(value = "", layout = OrgNavigationView.class)
@PermitAll
public class OrganisationsView extends Div implements BeforeEnterObserver {
    private final transient AuthenticationContext authenticationContext;
    private final OrganisationRepository organisationRepository;
    private final BoardsRepository boardsRepository;
    private final PersonsRepository personsRepository;

    public OrganisationsView(OrganisationsService organisationsService, BoardsService boardsService, PersonsService personsService, AuthenticationContext authenticationContext) {
        this.organisationRepository = organisationsService.getOrganisationRepository();
        this.boardsRepository = boardsService.getBoardsRepository();
        this.personsRepository  = personsService.getPersonsRepository();
        this.authenticationContext = authenticationContext;

        HorizontalLayout header = new HorizontalLayout();
        Button newOrg = new Button("Neue Organisation...");
        newOrg.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        newOrg.addClickShortcut(Key.ENTER);
        newOrg.setTooltipText("Öffnet einen neuen Dialog um eine Organisation zu erstellen");
        Grid<Organisation> grid = new Grid<>(Organisation.class, false);
        newOrg.addClickListener(neueOrgEvent -> createDialog(null, org -> {
            organisationRepository.save(org);
            grid.setItems(organisationRepository.findAll());
        }, id -> organisationRepository.findById(id).isPresent() ?
                "Die ID ist bereits vergeben" : "").open());
        header.add(newOrg);
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        add(header);
        grid.addColumn("name").setHeader("Name").setAutoWidth(true);
        grid.addColumn("id").setHeader("id").setAutoWidth(true);
        grid.addComponentColumn(organisation -> {
            String id = organisation.getId();
            Button edit = new Button(LumoIcon.EDIT.create());
            edit.addClickListener(editEvent -> createDialog(organisation,
                    org -> {
                        organisationRepository.save(org);
                        boardsRepository.findByOrg(id).forEach(board -> {
                            board.setOrganisation(org);
                            boardsRepository.save(board);
                        });
                        organisationRepository.deleteById(id);
                        grid.setItems(organisationRepository.findAll());
                    },
                    newID -> {
                        boolean result = organisationRepository.existsById(newID) && !organisation.getId().equals(id);
                        return result ? "Die ID ist bereits vergeben" : "";
                    }).open());
            return edit;
        }).setFlexGrow(0);
        grid.addComponentColumn(organisation -> {
            Button delete = new Button(VaadinIcon.TRASH.create());
            delete.addClickListener(event -> {
                ConfirmDialog confirmDialog = new ConfirmDialog();
                confirmDialog.setHeader("Delete " + organisation.getName() + "?");
                confirmDialog.setCancelable(true);
                confirmDialog.addConfirmListener(___ -> {
                    organisationRepository.deleteById(organisation.getId());
                    grid.setItems(organisationRepository.findAll());
                });
                confirmDialog.open();
            });
            return delete;
        }).setFlexGrow(0);
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_NO_BORDER);
        grid.setItems(organisationRepository.findAll());
        grid.setSelectionMode(Grid.SelectionMode.SINGLE).addSelectionListener(selectionEvent -> {
            selectionEvent.getFirstSelectedItem().ifPresent(org ->
                    selectionEvent.getSource().getUI().ifPresent(ui -> ui.navigate(BoardsView.class, new RouteParam("orgID", org.getId()))));
        });
        add(grid);
    }

    private static Dialog createDialog(@Nullable Organisation organisation, Consumer<Organisation> callback, Function<String, String> idValidator) {
        Dialog dialog = new Dialog();
        dialog.setDraggable(true);
        dialog.setHeaderTitle("Neue Organisation");
        TextField name = new TextField("Name");
        name.setRequired(true);
        TextField id = new TextField("ID");
        if (organisation != null) {
            name.setValue(organisation.getName());
            id.setValue(organisation.getId());
        }
        AtomicBoolean idChanged = new AtomicBoolean(false);
        name.addValueChangeListener(nameChangeEvent -> {
            if (!idChanged.get()) {
                id.setValue(nameChangeEvent.getValue().toLowerCase(Locale.ROOT).replace(" ", "-"));
            }
        });
        id.addValueChangeListener(idChangeEvent -> idChanged.set(true));
        Button cancel = new Button("Cancel");
        Button confirm = new Button("Confirm");
        confirm.addClickShortcut(Key.ENTER);
        confirm.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        cancel.addThemeVariants(ButtonVariant.LUMO_ERROR);
        dialog.add(new HorizontalLayout(name, id));
        dialog.getFooter().add(cancel, confirm);
        confirm.addClickListener(confirmEvent -> {
            boolean error = false;
            if (name.getValue().isBlank()) {
                name.setLabel("Der Name darf nicht leer sein");
                error = true;
            }
            if (id.getValue().isBlank()) {
                id.setLabel("Die ID darf nicht leer sein");
                error = true;
            }
            String idResult = idValidator.apply(id.getValue());
            if (!idResult.isBlank()) {
                id.setLabel(idResult);
                error = true;
            }
            if (!error) {
                if (organisation != null) {
                    organisation.setName(name.getValue());
                    organisation.setId(id.getValue());
                }
                Organisation newOrg = new Organisation(id.getValue(), name.getValue());
                callback.accept(organisation != null ? organisation : newOrg);
                dialog.close();
            }
        });
        cancel.addClickListener(cancelEvent -> dialog.close());
        name.setAutofocus(true);
        return dialog;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        Optional<UserDetails> optionalUserDetails = authenticationContext.getAuthenticatedUser(UserDetails.class);
        Optional<Person> optionalPerson = optionalUserDetails.flatMap(userDetails -> personsRepository.findByUsername(userDetails.getUsername()));
        if(optionalUserDetails.isEmpty() || optionalPerson.isEmpty()) {
            beforeEnterEvent.rerouteTo(NotFoundView.class);
            return;
        }
        AtomicBoolean allow = new AtomicBoolean(false);
        optionalUserDetails.get().getAuthorities().forEach(grantedAuthority -> {
            if(grantedAuthority.getAuthority().equalsIgnoreCase("ADMIN")) {
                allow.set(true);
            }
        });
        if(!allow.get()) {
            beforeEnterEvent.rerouteTo(BoardsView.class, new RouteParameters(new RouteParam("orgID", optionalPerson.get().getOrganisation().getId())));
        }
    }
}