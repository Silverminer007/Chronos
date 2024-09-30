package de.kjgstbarbara.views.gameevaluation;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MultiFileMemoryBuffer;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.theme.lumo.LumoIcon;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.kjgstbarbara.GameEvaluationUtils;
import de.kjgstbarbara.data.GameEvaluation;
import de.kjgstbarbara.data.Participant;
import de.kjgstbarbara.data.Person;
import de.kjgstbarbara.service.PersonsRepository;
import de.kjgstbarbara.services.GameEvaluationService;
import de.kjgstbarbara.services.GameGroupService;
import de.kjgstbarbara.services.ParticipantService;
import de.kjgstbarbara.views.MainNavigationView;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.vaadin.olli.FileDownloadWrapper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

@PermitAll
@Route(value = "gameevaluation/:game-evaluation/scoreboard", layout = MainNavigationView.class)
public class ScoreboardView extends Composite<VerticalLayout> implements BeforeEnterObserver {

    private final Grid<Participant> participants = new Grid<>();

    private GameEvaluation gameEvaluation;

    private final Person principal;

    public ScoreboardView(PersonsRepository personsRepository, AuthenticationContext authenticationContext) {
        this.principal = authenticationContext.getAuthenticatedUser(UserDetails.class)
                .flatMap(userDetails -> personsRepository.findByUsername(userDetails.getUsername()))
                .orElse(null);
        if (principal == null) {
            authenticationContext.logout();
        }
        getContent().setSizeFull();

        FlexLayout header = new FlexLayout();
        header.addClassName(LumoUtility.Gap.MEDIUM);
        header.setFlexWrap(FlexLayout.FlexWrap.WRAP);
        header.setAlignItems(FlexComponent.Alignment.CENTER);

        Button back = new Button();
        back.setAriaLabel("Vorherige Seite");
        back.addClickListener(event -> UI.getCurrent().navigate(GameEvaluationView.class, new RouteParameters(new RouteParam("game-evaluation", this.gameEvaluation.getId()))));
        back.setIcon(VaadinIcon.ARROW_LEFT.create());
        header.add(back);

        H2 title = new H2("Teilnehmende");
        header.add(title);

        HorizontalLayout menu = new HorizontalLayout();

        MultiFileMemoryBuffer multiFileMemoryBuffer = new MultiFileMemoryBuffer();

        Button uploadButton = new Button();
        uploadButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
        uploadButton.setTooltipText("Teilnehmende aus txt Importieren");
        uploadButton.setIcon(VaadinIcon.UPLOAD.create());
        uploadButton.setAriaLabel("Teilnahmeliste hochladen");
        Upload upload = new Upload(multiFileMemoryBuffer);
        upload.setDropAllowed(false);
        upload.setUploadButton(uploadButton);
        upload.addFileRejectedListener(event -> Notification.show(event.getErrorMessage()));
        upload.addFailedListener(event -> Notification.show(event.getReason().getMessage()));
        upload.addSucceededListener(event -> {
            List<Participant> participantList = importFromCSV(multiFileMemoryBuffer);
            for (Participant p : participantList) {
                participantService.update(p);
            }
            this.gameEvaluation.getParticipants().addAll(participantList);
            this.gameEvaluationService.update(this.gameEvaluation);
            this.gameEvaluation = this.gameEvaluationService.get(this.gameEvaluation.getId()).orElse(this.gameEvaluation);
            participants.setItems(this.gameEvaluation.getScoreBoard());
            Notification.show(participantList.size() + " Teilnehmende importiert");
        });
        menu.add(upload);

        Button addNew = new Button();
        addNew.setIcon(LumoIcon.PLUS.create());
        addNew.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
        addNew.setAriaLabel("Neuer Teilnehmender");
        addNew.addClickListener(event -> {
            Dialog dialog = new Dialog();
            dialog.setHeaderTitle("Neuer Teilnehmender");
            dialog.setCloseOnEsc(true);
            dialog.setCloseOnOutsideClick(true);

            TextField name = new TextField("Name");
            name.focus();
            dialog.add(name);

            Button save = new Button("Erstellen");
            save.addClickShortcut(Key.ENTER);
            save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            save.addClickListener(saveEvent -> {
                Participant participant = new Participant();
                participant.setName(name.getValue());
                participantService.update(participant);
                gameEvaluation.getParticipants().add(participant);
                this.gameEvaluation = gameEvaluationService.update(gameEvaluation);
                participants.setItems(this.gameEvaluation.getScoreBoard());
                dialog.close();
            });
            dialog.getFooter().add(save);
            dialog.open();
        });
        menu.add(addNew);

        Button export = new Button();
        export.setAriaLabel("Scoreboard als xlsx exportieren");
        export.setIcon(VaadinIcon.DOWNLOAD.create());
        export.addThemeVariants(ButtonVariant.LUMO_LARGE, ButtonVariant.LUMO_PRIMARY);
        FileDownloadWrapper exportWrapper = new FileDownloadWrapper(
                new StreamResource("Spieleauswertung.xlsx", () -> {
                    ByteArrayOutputStream os = new ByteArrayOutputStream();
                    try {
                        GameEvaluationUtils.createScoreboard(this.gameEvaluation, os);
                        return new ByteArrayInputStream(os.toByteArray());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
        );
        exportWrapper.wrapComponent(export);
        menu.add(exportWrapper);

        header.add(menu);

        getContent().add(header);

        participants.setSelectionMode(Grid.SelectionMode.SINGLE);
        participants.addSelectionListener(event ->
                event.getFirstSelectedItem().ifPresent(participant ->
                        UI.getCurrent().navigate(ParticipantView.class,
                                new RouteParameters(
                                        new RouteParam("game-evaluation", this.gameEvaluation.getId()),
                                        new RouteParam("participant", participant.getId())))));
        participants.setHeightFull();
        participants.setWidthFull();
        participants.addColumn(person -> (this.gameEvaluation.getScoreBoard().indexOf(person) + 1) + ".").setAutoWidth(true);
        participants.addColumn(Participant::getName).setAutoWidth(true);
        participants.addColumn(person -> this.gameEvaluation.getPointsOf(person) + " Pkt").setTextAlign(ColumnTextAlign.END).setWidth("min-content");
        participants.addColumn(participant -> Math.round(this.gameEvaluation.getScoreOf(participant) * 100) + "%").setTextAlign(ColumnTextAlign.END).setWidth("min-content");
        participants.addComponentColumn(person -> {
            Button delete = new Button();
            delete.setAriaLabel("Teilnehmer entfernen");
            delete.setIcon(VaadinIcon.TRASH.create());
            delete.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
            delete.addClickListener(deleteEvent -> {
                this.gameEvaluation.getParticipants().remove(person);
                this.gameEvaluationService.update(this.gameEvaluation);
                this.gameGroupService.getByPerson(person).forEach(group -> {
                    group.getParticipants().remove(person);
                    gameGroupService.update(group);
                });
                this.participantService.delete(person.getId());
                this.gameEvaluation = this.gameEvaluationService.get(this.gameEvaluation.getId()).orElse(this.gameEvaluation);
                participants.setItems(this.gameEvaluation.getScoreBoard());
            });
            return delete;
        }).setTextAlign(ColumnTextAlign.END);
        getContent().add(participants);
    }

    @Autowired
    private GameEvaluationService gameEvaluationService;
    @Autowired
    private GameGroupService gameGroupService;
    @Autowired
    private ParticipantService participantService;

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        this.gameEvaluation = beforeEnterEvent.getRouteParameters().get("game-evaluation").map(Long::valueOf).flatMap(gameEvaluationService::get).orElse(null);
        if (gameEvaluation == null || !gameEvaluation.getGroup().getMembers().contains(this.principal)) {
            beforeEnterEvent.rerouteTo("gameevaluation");
            return;
        }
        participants.setItems(this.gameEvaluation.getScoreBoard());
    }

    private List<Participant> importFromCSV(MultiFileMemoryBuffer multiFileMemoryBuffer) {
        List<Participant> participantList = new ArrayList<>();
        for (String file : multiFileMemoryBuffer.getFiles()) {
            Scanner scanner = new Scanner(multiFileMemoryBuffer.getInputStream(file));
            while (scanner.hasNext()) {
                Participant participant = new Participant();
                participant.setName(scanner.nextLine());
                participantList.add(participant);
            }
        }
        return participantList;
    }
}