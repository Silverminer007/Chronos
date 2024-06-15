package de.kjgstbarbara.gameevaluation.views;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.*;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.theme.lumo.LumoIcon;
import com.vaadin.flow.theme.lumo.LumoUtility.Gap;
import de.kjgstbarbara.data.Person;
import de.kjgstbarbara.gameevaluation.data.Game;
import de.kjgstbarbara.gameevaluation.data.GameEvaluation;
import de.kjgstbarbara.gameevaluation.data.Participant;
import de.kjgstbarbara.gameevaluation.services.GameEvaluationService;
import de.kjgstbarbara.gameevaluation.services.GameService;
import de.kjgstbarbara.service.PersonsRepository;
import de.kjgstbarbara.views.nav.MainNavigationView;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

@PermitAll
@Route(value = "gameevaluation/:game-evaluation", layout = MainNavigationView.class)
public class GameEvaluationView extends Composite<VerticalLayout> implements BeforeEnterObserver {

    private final H2 title = new H2();
    private final Grid<Game> gameList = new Grid<>();
    private final VerticalLayout scoreBoard = new VerticalLayout();

    private GameEvaluation gameEvaluation;

    private final Person principal;

    public GameEvaluationView(PersonsRepository personsRepository, AuthenticationContext authenticationContext) {
        this.principal = authenticationContext.getAuthenticatedUser(UserDetails.class)
                .flatMap(userDetails -> personsRepository.findByUsername(userDetails.getUsername()))
                .orElse(null);
        if (principal == null) {
            authenticationContext.logout();
        }

        getContent().setWidth("100%");
        getContent().setHeightFull();
        getContent().getStyle().set("flex-grow", "1");

        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setAlignItems(Alignment.CENTER);

        Button back = new Button();
        back.setAriaLabel("Vorherige Seite");
        back.addClickListener(event -> UI.getCurrent().navigate(GameEvaluationListView.class));
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
            this.gameEvaluation.setName(editNameField.getValue());
            this.gameEvaluation = this.gameEvaluationService.update(this.gameEvaluation);
            this.title.setText(this.gameEvaluation.getName());
            this.title.setVisible(true);
            editNameField.setVisible(false);
            rename.setVisible(true);
            editNameDoneButton.setVisible(false);
        });

        getContent().add(header);

        getContent().add(new H2("Scoreboard"));

        scoreBoard.addClickListener(event ->
                UI.getCurrent().navigate(ScoreboardView.class,
                        new RouteParameters(new RouteParam("game-evaluation", gameEvaluation.getId()))));
        scoreBoard.setWidthFull();
        getContent().add(scoreBoard);

        Hr hr = new Hr();
        getContent().add(hr);

        HorizontalLayout gamesHeader = new HorizontalLayout();
        gamesHeader.setAlignSelf(Alignment.START);
        gamesHeader.setAlignItems(Alignment.CENTER);
        gamesHeader.setWidthFull();
        gamesHeader.addClassName(Gap.MEDIUM);
        gamesHeader.setWidth("100%");
        gamesHeader.getStyle().set("flex-grow", "1");

        H2 gamesTitle = new H2();
        gamesTitle.setText("Spiele");
        gamesTitle.setWidth("max-content");
        gamesHeader.add(gamesTitle);

        Button newGame = new Button();
        newGame.setAriaLabel("Neues Spiel");
        newGame.setIcon(LumoIcon.PLUS.create());
        newGame.setWidth("min-content");
        newGame.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        newGame.addClickListener(event -> {
            Dialog dialog = new Dialog();
            dialog.setHeaderTitle("Neues Spiel");
            dialog.setCloseOnEsc(true);
            dialog.setCloseOnOutsideClick(true);

            TextField name = new TextField("Name");
            name.focus();
            dialog.add(name);

            Button save = new Button("Erstellen");
            save.addClickShortcut(Key.ENTER);
            save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            save.addClickListener(saveEvent -> {
                Game game = new Game();
                game.setName(name.getValue());
                gameService.update(game);
                gameEvaluation.getGames().add(game);
                this.gameEvaluation = gameEvaluationService.update(gameEvaluation);
                gameList.setItems(gameEvaluation.getGames());
                dialog.close();
            });
            dialog.getFooter().add(save);
            dialog.open();
        });
        gamesHeader.add(newGame);

        getContent().add(gamesHeader);

        gameList.setWidthFull();
        gameList.setHeightFull();
        gameList.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_NO_ROW_BORDERS);
        gameList.addComponentColumn(game -> {
            H3 gameTitle = new H3(game.getName());
            gameTitle.addClickListener(event ->
                    UI.getCurrent().navigate(GameView.class,
                            new RouteParameters(
                                    new RouteParam("game-evaluation", gameEvaluation.getId()),
                                    new RouteParam("game", game.getId()))));
            return gameTitle;
        });
        gameList.addComponentColumn(game -> {
            Button delete = new Button();
            delete.setAriaLabel("Spiel entfernen");
            delete.setIcon(VaadinIcon.TRASH.create());
            delete.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
            delete.addClickListener(deleteEvent -> {
                this.gameEvaluation.getGames().remove(game);
                this.gameEvaluation = this.gameEvaluationService.update(this.gameEvaluation);
                this.gameService.delete(game.getId());
                gameList.setItems(this.gameEvaluation.getGames());
                updateScoreboard();
            });
            return delete;
        }).setTextAlign(ColumnTextAlign.END).setAutoWidth(true);
        getContent().add(gameList);
    }

    @Autowired
    private GameEvaluationService gameEvaluationService;
    @Autowired
    private GameService gameService;

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        this.gameEvaluation = beforeEnterEvent.getRouteParameters().get("game-evaluation").map(Long::valueOf).flatMap(gameEvaluationService::get).orElse(null);
        if (gameEvaluation == null || !gameEvaluation.getGroup().getMembers().contains(this.principal)) {
            beforeEnterEvent.rerouteTo("gameevaluation");
            return;
        }
        this.gameList.setItems(gameEvaluation.getGames());
        updateScoreboard();
    }

    private void updateScoreboard() {
        this.scoreBoard.removeAll();
        List<Participant> participants = gameEvaluation.getScoreBoard();
        for (int i = 0; i < 3; i++) {
            if (participants.size() > i) {
                this.scoreBoard.add(new H5((i + 1) + ". " + participants.get(i).getName() + " (" + this.gameEvaluation.getPointsOf(participants.get(i)) + " Pkt - " + Math.round(this.gameEvaluation.getScoreOf(participants.get(i)) * 100) + "%)"));
            }
        }
        if (participants.isEmpty()) {
            this.scoreBoard.add("Keine Teilnehmenden bisher");
        }
        this.title.setText(gameEvaluation.getName());
    }
}
