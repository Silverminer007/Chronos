package de.kjgstbarbara.views;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.router.*;
import com.vaadin.flow.spring.data.VaadinSpringDataHelpers;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.theme.lumo.LumoIcon;
import com.vaadin.flow.theme.lumo.LumoUtility.Gap;
import de.kjgstbarbara.data.Group;
import de.kjgstbarbara.data.Organisation;
import de.kjgstbarbara.data.Person;
import de.kjgstbarbara.data.GameEvaluation;
import de.kjgstbarbara.services.GameEvaluationService;
import de.kjgstbarbara.service.GroupService;
import de.kjgstbarbara.service.OrganisationService;
import de.kjgstbarbara.service.PersonsService;
import de.kjgstbarbara.components.NonNullValidator;
import de.kjgstbarbara.views.gameevaluation.GameEvaluationView;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.userdetails.UserDetails;

@PermitAll
@Route(value = "gameevaluation", layout = MainNavigationView.class)
@Uses(Icon.class)
public class GameEvaluationListView extends Composite<VerticalLayout> {

    private final Person principal;

    public GameEvaluationListView(OrganisationService organisationService, GroupService groupService, PersonsService personsService, AuthenticationContext authenticationContext) {
        this.principal = authenticationContext.getAuthenticatedUser(UserDetails.class)
                .flatMap(userDetails -> personsService.getPersonsRepository().findByUsername(userDetails.getUsername()))
                .orElse(null);
        if (principal == null) {
            authenticationContext.logout();
        }

        getContent().setWidth("100%");
        getContent().getStyle().set("flex-grow", "1");

        HorizontalLayout header = new HorizontalLayout();
        header.addClassName(Gap.MEDIUM);
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setWidth("100%");
        header.setHeight("min-content");

        H2 title = new H2();
        title.setText("Spieleauswertungen");
        title.setWidth("max-content");
        header.add(title);

        Button addNew = new Button();
        addNew.setIcon(LumoIcon.PLUS.create());
        addNew.setAriaLabel("New Game Evaluation");
        addNew.setWidth("min-content");
        addNew.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
        header.add(addNew);

        getContent().add(header);

        getContent().setHeightFull();
        Grid<GameEvaluation> stripedGrid = new Grid<>();
        stripedGrid.setSelectionMode(Grid.SelectionMode.NONE);
        stripedGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_NO_ROW_BORDERS);
        stripedGrid.setWidth("100%");
        stripedGrid.setHeight("100%");
        setGridData(stripedGrid);
        getContent().add(stripedGrid);

        stripedGrid.addComponentColumn(gameEvaluation -> {
            VerticalLayout gameEvaluationWidget = new VerticalLayout();
            HorizontalLayout primaryLine = new HorizontalLayout();
            primaryLine.setAlignItems(FlexComponent.Alignment.CENTER);
            primaryLine.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
            H3 name = new H3(gameEvaluation.getName());
            primaryLine.add(name);
            Button share = new Button();
            share.setVisible(false);// TODO Brauche ich das überhaupt? Wahrscheinlich ja, denn personenbezogenen Daten sollten nicht ohne Passwort zugänglich sein
            share.setIcon(VaadinIcon.SHARE.create());
            primaryLine.add(share);
            gameEvaluationWidget.add(primaryLine);
            H4 first = new H4(!gameEvaluation.getParticipants().isEmpty()
                    ? "Erster: " + gameEvaluation.getScoreBoard().getFirst().getName()
                    : "Keine Teilnehmenden");
            gameEvaluationWidget.add(first);
            gameEvaluationWidget.addClickListener(event ->
                    UI.getCurrent().navigate(GameEvaluationView.class,
                            new RouteParameters(new RouteParam("game-evaluation", gameEvaluation.getId()))));
            return gameEvaluationWidget;
        });
        stripedGrid.addComponentColumn(gameEvaluation -> {
            Button delete = new Button();
            delete.setAriaLabel("Spieleauswertung entfernen");
            delete.setIcon(VaadinIcon.TRASH.create());
            delete.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
            delete.addClickListener(deleteEvent -> {
                this.gameEvaluationService.delete(gameEvaluation.getId());
                this.setGridData(stripedGrid);
            });
            return delete;
        }).setTextAlign(ColumnTextAlign.END).setAutoWidth(true);

        addNew.addClickListener(event -> {
            Dialog dialog = new Dialog();
            dialog.setHeaderTitle("Neue Spieleauswertung");

            GameEvaluation gameEvaluation = new GameEvaluation();
            Binder<GameEvaluation> binder = new Binder<>();

            VerticalLayout content = new VerticalLayout();

            TextField name = new TextField("Name");
            name.focus();
            binder.forField(name).bind(GameEvaluation::getName, GameEvaluation::setName);
            content.add(name);

            ComboBox<Group> selectGroup = new ComboBox<>();
            selectGroup.setLabel("Gruppe");
            selectGroup.setRequired(true);
            selectGroup.setAllowCustomValue(true);
            selectGroup.addCustomValueSetListener(e -> {
                Group group = new Group();
                group.setName(e.getDetail());
                group.getAdmins().add(principal);
                group.getMembers().add(principal);
                selectGroup.setValue(group);
            });
            binder.forField(selectGroup).withValidator(new NonNullValidator<>()).bind(GameEvaluation::getGroup, GameEvaluation::setGroup);
            selectGroup.setItems(groupService.getGroupRepository().findByMembersIn(this.principal));
            content.add(selectGroup);

            ComboBox<Organisation> selectOrganisation = new ComboBox<>();
            selectOrganisation.setLabel("Organisation");
            selectOrganisation.setVisible(false);
            selectOrganisation.setRequired(true);
            selectOrganisation.setAllowCustomValue(true);
            selectOrganisation.setItems(organisationService.getOrganisationRepository().findByMembersIn(this.principal));
            selectOrganisation.addCustomValueSetListener(e -> {
                Organisation organisation = new Organisation();
                organisation.setName(e.getDetail());
                organisation.setAdmin(this.principal);
                organisation.getMembers().add(this.principal);
                selectOrganisation.setValue(organisation);
            });
            content.add(selectOrganisation);

            selectGroup.addValueChangeListener(e -> selectOrganisation.setVisible(e.getValue().getOrganisation() == null));

            dialog.add(content);

            binder.readBean(gameEvaluation);
            Button save = new Button("Erstellen");
            save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            save.addClickShortcut(Key.ENTER);
            save.addClickListener(clickEvent -> {
                try {
                    binder.writeBean(gameEvaluation);
                    if (selectOrganisation.isVisible()) {
                        if (selectOrganisation.getValue() != null) {
                            if (gameEvaluation.getGroup().getOrganisation() == null) {
                                organisationService.getOrganisationRepository().save(selectOrganisation.getValue());
                                gameEvaluation.getGroup().setOrganisation(selectOrganisation.getValue());
                            }
                        } else {
                            selectOrganisation.setInvalid(true);
                            selectOrganisation.setErrorMessage("Bitte wähle eine Organisation aus, zu der die neue Gruppe gehören soll");
                            return;
                        }
                    }
                    groupService.getGroupRepository().save(gameEvaluation.getGroup());
                    gameEvaluationService.update(gameEvaluation);
                    setGridData(stripedGrid);
                    dialog.close();
                } catch (ValidationException ignored) {
                }
            });
            dialog.getFooter().add(save);
            dialog.setCloseOnOutsideClick(true);
            dialog.setCloseOnEsc(true);
            dialog.open();
        });
    }

    private void setGridData(Grid<GameEvaluation> grid) {
        grid.setItems(query -> gameEvaluationService.listForPerson(
                        PageRequest.of(query.getPage(), query.getPageSize(), VaadinSpringDataHelpers.toSpringDataSort(query)), this.principal)
                .stream());
    }

    @Autowired()
    private GameEvaluationService gameEvaluationService;
}
