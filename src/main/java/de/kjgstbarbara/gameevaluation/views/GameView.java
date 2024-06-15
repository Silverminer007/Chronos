package de.kjgstbarbara.gameevaluation.views;

import com.itextpdf.text.DocumentException;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H5;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.theme.lumo.LumoIcon;
import com.vaadin.flow.theme.lumo.LumoUtility.Gap;
import de.kjgstbarbara.data.Person;
import de.kjgstbarbara.gameevaluation.Utils;
import de.kjgstbarbara.gameevaluation.data.Game;
import de.kjgstbarbara.gameevaluation.data.GameEvaluation;
import de.kjgstbarbara.gameevaluation.data.GameGroup;
import de.kjgstbarbara.gameevaluation.data.Participant;
import de.kjgstbarbara.gameevaluation.services.GameEvaluationService;
import de.kjgstbarbara.gameevaluation.services.GameGroupService;
import de.kjgstbarbara.gameevaluation.services.GameService;
import de.kjgstbarbara.service.PersonsRepository;
import de.kjgstbarbara.views.nav.MainNavigationView;
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

    private final H1 title = new H1();
    private final VerticalLayout groups = new VerticalLayout();

    private final Person principal;

    public GameView(PersonsRepository personsRepository, AuthenticationContext authenticationContext) {
        this.principal = authenticationContext.getAuthenticatedUser(UserDetails.class)
                .flatMap(userDetails -> personsRepository.findByUsername(userDetails.getUsername()))
                .orElse(null);
        if (principal == null) {
            authenticationContext.logout();
        }
        HorizontalLayout header = new HorizontalLayout();
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

        Button newGroup = new Button();
        newGroup.setAriaLabel("Neue Gruppe");
        newGroup.setIcon(LumoIcon.PLUS.create());
        newGroup.addThemeVariants(ButtonVariant.LUMO_LARGE, ButtonVariant.LUMO_PRIMARY);
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
                updateGroupAccordion();
                dialog.close();
            });
            dialog.getFooter().add(save);
            dialog.open();
        });
        header.add(newGroup);

        Button shuffleGroups = new Button();
        shuffleGroups.setIcon(VaadinIcon.REFRESH.create());
        shuffleGroups.setAriaLabel("Gruppen mischen");
        shuffleGroups.setWidth("min-content");
        shuffleGroups.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
        shuffleGroups.addClickListener(event -> {
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
            updateGroupAccordion();
        });
        header.add(shuffleGroups);

        Button export = new Button();
        export.setAriaLabel("Gruppen exportieren");
        export.setIcon(VaadinIcon.DOWNLOAD.create());
        export.addThemeVariants(ButtonVariant.LUMO_LARGE, ButtonVariant.LUMO_PRIMARY);
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
                            Utils.createGroupOverview(this.game, os);
                            Utils.xlsxToPdf(new ByteArrayInputStream(os.toByteArray()), os);
                            return new ByteArrayInputStream(os.toByteArray());
                        } catch (IOException | DocumentException e) {
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
                            Utils.createGroupOverview(this.game, os);
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
        updateGroupAccordion();
    }

    public void updateGroupAccordion() {
        Accordion groupAccordion = new Accordion();
        groupAccordion.setWidthFull();
        for (GameGroup g : this.game.getGameGroups()) {
            VerticalLayout groupWidget = new VerticalLayout();
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
            for (Participant p : g.getParticipants()) {
                groupWidget.add(new H5(p.getName()));
            }
            groupAccordion.add(g.getName() + " (" + g.getParticipants().size() + " Teilnehmende)", groupWidget);
        }
        this.groups.removeAll();
        this.groups.add(groupAccordion);
    }
}
