package de.kjgstbarbara.views.gameevaluation;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H5;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.theme.lumo.LumoIcon;
import com.vaadin.flow.theme.lumo.LumoUtility.Gap;
import de.kjgstbarbara.data.Person;
import de.kjgstbarbara.GameEvaluationUtils;
import de.kjgstbarbara.data.Game;
import de.kjgstbarbara.data.GameEvaluation;
import de.kjgstbarbara.data.GameGroup;
import de.kjgstbarbara.data.Participant;
import de.kjgstbarbara.services.GameEvaluationService;
import de.kjgstbarbara.services.GameGroupService;
import de.kjgstbarbara.services.GameService;
import de.kjgstbarbara.service.PersonsRepository;
import de.kjgstbarbara.views.MainNavigationView;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.vaadin.olli.FileDownloadWrapper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@PermitAll
@Route(value = "gameevaluation/:game-evaluation/game/:game", layout = MainNavigationView.class)
public class GameView extends Composite<VerticalLayout> implements BeforeEnterObserver {

    private GameEvaluation gameEvaluation;
    private Game game;

    private final H2 title = new H2();
    private final VerticalLayout groups = new VerticalLayout();

    private final Person principal;

    public GameView(PersonsRepository personsRepository, AuthenticationContext authenticationContext) {
        this.principal = authenticationContext.getAuthenticatedUser(UserDetails.class)
                .flatMap(userDetails -> personsRepository.findByUsername(userDetails.getUsername()))
                .orElse(null);
        if (principal == null) {
            authenticationContext.logout();
        }
        FlexLayout header = new FlexLayout();
        header.setFlexWrap(FlexLayout.FlexWrap.WRAP);
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.addClassName(Gap.MEDIUM);
        header.setWidth("100%");
        header.setHeight("min-content");

        Button back = new Button();
        back.setAriaLabel("Vorherige Seite");
        back.addClickListener(event -> UI.getCurrent().navigate(GameEvaluationView.class, new RouteParameters(new RouteParam("game-evaluation", this.gameEvaluation.getId()))));
        back.setIcon(VaadinIcon.ARROW_LEFT.create());
        header.add(back);

        header.add(title);

        TextField editNameField = new TextField();
        editNameField.setVisible(false);
        header.add(editNameField);

        Button editNameDoneButton = new Button();
        editNameDoneButton.setIcon(VaadinIcon.CHECK.create());
        editNameDoneButton.setVisible(false);
        header.add(editNameDoneButton);

        Button rename = new Button();
        rename.setIcon(VaadinIcon.PENCIL.create());
        rename.setAriaLabel("Namen bearbeiten");
        rename.addClickListener(event -> {
            title.setVisible(false);
            editNameField.setVisible(true);
            editNameField.setValue(title.getText());
            rename.setVisible(false);
            editNameDoneButton.setVisible(true);
        });
        header.add(rename);

        editNameDoneButton.addClickListener(event -> {
            this.game.setName(editNameField.getValue());
            this.game = this.gameService.update(this.game);
            this.title.setText(this.game.getName());
            this.title.setVisible(true);
            editNameField.setVisible(false);
            rename.setVisible(true);
            editNameDoneButton.setVisible(false);
        });

        Button shuffleGroups = new Button();
        shuffleGroups.setIcon(VaadinIcon.REFRESH.create());
        shuffleGroups.setAriaLabel("Gruppen mischen");
        shuffleGroups.setWidth("min-content");
        shuffleGroups.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        shuffleGroups.addClickListener(event -> {
            if(!this.game.getGameGroups().isEmpty()) {
                List<Participant> participantList = new ArrayList<>(this.gameEvaluation.getParticipants());
                Collections.shuffle(participantList);
                for (GameGroup g : this.game.getGameGroups()) {
                    g.setParticipants(new ArrayList<>());
                }
                for (int i = 0; i < participantList.size(); i++) {
                    this.game.getGameGroups().get(i % this.game.getGameGroups().size()).getParticipants().add(participantList.get(i));
                }
                for (GameGroup g : this.game.getGameGroups()) {
                    this.gameGroupService.update(g);
                }
                this.gameService.update(this.game);
                this.game = this.gameService.get(this.game.getId()).orElse(this.game);
                updateGroupTabs(null);
            } else {
                Notification.show("Bitte erstelle zuerst eine oder mehrere Gruppen").addThemeVariants(NotificationVariant.LUMO_WARNING);
            }
        });
        header.add(shuffleGroups);

        Button export = new Button();
        export.setAriaLabel("Gruppen exportieren");
        export.setIcon(VaadinIcon.DOWNLOAD.create());
        export.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        export.addClickListener(event -> {
            Dialog exportDialog = new Dialog();
            exportDialog.setHeaderTitle("Gruppen exportieren");
            exportDialog.setCloseOnOutsideClick(true);
            exportDialog.setCloseOnEsc(true);

            Button pdf = new Button();
            pdf.setText("Als PDF");
            pdf.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            FileDownloadWrapper pdfExportWrapper = new FileDownloadWrapper(
                    new StreamResource(this.game.getName() + ".pdf", () -> {
                        ByteArrayOutputStream os = new ByteArrayOutputStream();
                        try {
                            GameEvaluationUtils.createGroupOverview(this.game, os);
                            GameEvaluationUtils.xlsxToPdf(new ByteArrayInputStream(os.toByteArray()), os);
                            return new ByteArrayInputStream(os.toByteArray());
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    })
            );
            pdfExportWrapper.wrapComponent(pdf);
            exportDialog.getFooter().add(pdfExportWrapper);
            Button xlsx = new Button();
            xlsx.setText("Als Excel Tabelle");
            xlsx.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            FileDownloadWrapper xlsxExportWrapper = new FileDownloadWrapper(
                    new StreamResource(this.game.getName() + ".xlsx", () -> {
                        ByteArrayOutputStream os = new ByteArrayOutputStream();
                        try {
                            GameEvaluationUtils.createGroupOverview(this.game, os);
                            return new ByteArrayInputStream(os.toByteArray());
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    })
            );
            xlsxExportWrapper.wrapComponent(xlsx);
            exportDialog.getFooter().add(xlsxExportWrapper);
            exportDialog.open();
        });
        header.add(export);

        getContent().add(header);

        getContent().setWidth("100%");
        getContent().getStyle().set("flex-grow", "1");

        groups.setWidth("100%");
        getContent().add(groups);
    }

    @Autowired
    private GameEvaluationService gameEvaluationService;
    @Autowired
    private GameService gameService;
    @Autowired
    private GameGroupService gameGroupService;

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        this.gameEvaluation = beforeEnterEvent.getRouteParameters().get("game-evaluation").map(Long::valueOf).flatMap(gameEvaluationService::get).orElse(null);
        if (gameEvaluation == null || !gameEvaluation.getGroup().getMembers().contains(this.principal)) {
            beforeEnterEvent.rerouteTo("gameevaluation");
            return;
        }
        this.game = beforeEnterEvent.getRouteParameters().get("game").map(Long::valueOf).flatMap(gameService::get).orElse(null);
        if (game == null) {
            beforeEnterEvent.rerouteTo("gameevaluation/" + gameEvaluation.getId());
            return;
        }
        this.title.setText(this.game.getName());
        updateGroupTabs(null);
    }

    public void updateGroupTabs(GameGroup openTab) {
        TabSheet groupTabs = new TabSheet();
        groupTabs.setSizeFull();
        for (GameGroup g : this.game.getGameGroups()) {
            VerticalLayout groupWidget = new VerticalLayout();
            groupWidget.setSizeFull();

            HorizontalLayout points = new HorizontalLayout();
            points.setAlignItems(FlexComponent.Alignment.CENTER);
            points.add(new H5("Punkte: "));
            IntegerField pointsField = new IntegerField();
            pointsField.setValue(g.getPoints());
            pointsField.addValueChangeListener(event -> {
                if (event.getValue() != null) {
                    g.setPoints(event.getValue());
                } else {
                    g.setPoints(0);
                }
                this.gameGroupService.update(g);
                this.game = this.gameService.get(this.game.getId()).orElse(this.game);
            });
            points.add(pointsField);
            groupWidget.add(points);

            HorizontalLayout addParticipants = new HorizontalLayout();

            MultiSelectComboBox<Participant> addParticipantsSelection = new MultiSelectComboBox<>();
            List<Participant> addableParticipants = new ArrayList<>(this.gameEvaluation.getParticipants());
            for (GameGroup gameGroup : this.game.getGameGroups()) {
                addableParticipants.removeAll(gameGroup.getParticipants());
            }
            addParticipantsSelection.setItemLabelGenerator(Participant::getName);
            addParticipantsSelection.setItems(addableParticipants);
            addParticipants.add(addParticipantsSelection);

            Button addParticipantsButton = new Button();
            addParticipantsButton.setAriaLabel("Ausgewählte zur Gruppe hinzufügen");
            addParticipantsButton.setIcon(LumoIcon.PLUS.create());
            addParticipantsButton.addClickListener(event -> {
                g.getParticipants().addAll(addParticipantsSelection.getValue());
                this.gameGroupService.update(g);
                this.game = this.gameService.get(this.game.getId()).orElse(this.game);
                this.updateGroupTabs(g);
            });
            addParticipants.add(addParticipantsButton);
            groupWidget.add(addParticipants);

            Grid<Participant> participantGrid = new Grid<>();
            participantGrid.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_ROW_STRIPES);
            participantGrid.setSelectionMode(Grid.SelectionMode.NONE);
            participantGrid.addColumn(Participant::getName);
            participantGrid.addComponentColumn(participant -> {
                Button moveTo = new Button();
                moveTo.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
                moveTo.setAriaLabel("In eine andere Gruppe verschieben");
                moveTo.setIcon(VaadinIcon.EXCHANGE.create());
                moveTo.addClickListener(event -> {
                    Dialog moveToSelection = new Dialog();
                    moveToSelection.setCloseOnEsc(true);
                    moveToSelection.setCloseOnOutsideClick(true);
                    moveToSelection.setHeaderTitle(participant.getName() + " in eine andere Gruppe verschieben");

                    RadioButtonGroup<GameGroup> groupSelection = new RadioButtonGroup<>();
                    groupSelection.setItems(this.game.getGameGroups());
                    groupSelection.setValue(g);
                    groupSelection.setItemLabelGenerator(GameGroup::getName);
                    moveToSelection.add(groupSelection);

                    Button move = new Button();
                    move.setText("Verschieben");
                    move.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                    move.addClickListener(e -> {
                        g.getParticipants().remove(participant);
                        this.gameGroupService.update(g);
                        groupSelection.getValue().getParticipants().add(participant);
                        this.gameGroupService.update(groupSelection.getValue());
                        this.game = this.gameService.get(this.game.getId()).orElse(this.game);
                        moveToSelection.close();
                        this.updateGroupTabs(g);
                    });
                    moveToSelection.getFooter().add(move);
                    moveToSelection.open();
                });
                return moveTo;
            }).setWidth("20px").setTextAlign(ColumnTextAlign.END);
            participantGrid.addComponentColumn(participant -> {
                Button remove = new Button();
                remove.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
                remove.setAriaLabel("Aus Gruppe entfernen");
                remove.setIcon(VaadinIcon.CLOSE.create());
                remove.addClickListener(event -> {
                    GameGroup gameGroup = this.gameGroupService.get(g.getId()).orElse(g);
                    gameGroup.getParticipants().remove(participant);
                    this.gameGroupService.update(gameGroup);
                    this.game = this.gameService.get(this.game.getId()).orElse(this.game);
                    this.updateGroupTabs(gameGroup);
                });
                return remove;
            }).setTextAlign(ColumnTextAlign.END).setWidth("20px");
            participantGrid.setWidthFull();
            participantGrid.setItems(g.getParticipants());
            groupWidget.add(participantGrid);

            HorizontalLayout editLine = new HorizontalLayout();

            TextField enterName = new TextField();
            enterName.setVisible(false);
            editLine.add(enterName);

            Button saveNewName = new Button();
            saveNewName.setAriaLabel("Neuen Namen speichern");
            saveNewName.setIcon(VaadinIcon.CHECK.create());
            saveNewName.setVisible(false);
            editLine.add(saveNewName);

            Button editName = new Button("Namen ändern");
            editName.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            editName.addClickListener(event -> {
                enterName.setVisible(true);
                saveNewName.setVisible(true);
                editName.setVisible(false);
                enterName.setValue(g.getName());
            });
            editLine.add(editName);

            saveNewName.addClickListener(event -> {
                enterName.setVisible(false);
                saveNewName.setVisible(false);
                editName.setVisible(true);
                g.setName(enterName.getValue());
                this.gameGroupService.update(g);
                this.updateGroupTabs(g);
            });

            Button delete = new Button("Gruppe löschen");
            delete.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
            delete.addClickListener(event -> {
                this.game.getGameGroups().remove(g);
                this.game = this.gameService.update(this.game);
                this.gameGroupService.delete(g.getId());
                this.updateGroupTabs(null);
            });
            editLine.add(delete);

            groupWidget.add(editLine);

            Tab tabTitle = new Tab(new Span(g.getName()), createBadge(g.getParticipants().size()));
            groupTabs.add(tabTitle, groupWidget);
            if (openTab != null && g.getId().equals(openTab.getId()))
                groupTabs.setSelectedTab(tabTitle);
        }
        Button newGroup = new Button();
        newGroup.setAriaLabel("Neue Gruppe");
        newGroup.setIcon(LumoIcon.PLUS.create());
        newGroup.addClickListener(event -> {
            Dialog dialog = new Dialog();
            dialog.setHeaderTitle("Neue Gruppe");
            dialog.setCloseOnEsc(true);
            dialog.setCloseOnOutsideClick(true);

            TextField name = new TextField("Name");
            name.focus();
            dialog.add(name);

            Button save = new Button("Erstellen");
            save.addClickShortcut(Key.ENTER);
            save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            save.addClickListener(saveEvent -> {
                GameGroup gameGroup = new GameGroup();
                gameGroup.setName(name.getValue());
                this.gameGroupService.update(gameGroup);
                this.game.getGameGroups().add(gameGroup);
                this.gameService.update(this.game);
                this.gameEvaluation = this.gameEvaluationService.get(this.gameEvaluation.getId()).orElse(this.gameEvaluation);
                this.game = this.gameService.get(this.game.getId()).orElse(this.game);
                updateGroupTabs(gameGroup);
                dialog.close();
            });
            dialog.getFooter().add(save);
            dialog.open();
        });
        groupTabs.setSuffixComponent(newGroup);
        this.groups.removeAll();
        this.groups.add(groupTabs);
    }

    private Span createBadge(int value) {
        Span badge = new Span(String.valueOf(value));
        badge.getElement().getThemeList().add("badge small contrast");
        badge.getStyle().set("margin-inline-start", "var(--lumo-space-xs)");
        return badge;
    }

}
