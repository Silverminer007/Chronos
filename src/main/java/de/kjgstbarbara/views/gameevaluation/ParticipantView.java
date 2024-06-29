package de.kjgstbarbara.views.gameevaluation;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.*;
import com.vaadin.flow.spring.security.AuthenticationContext;
import de.kjgstbarbara.data.Person;
import de.kjgstbarbara.data.Game;
import de.kjgstbarbara.data.GameEvaluation;
import de.kjgstbarbara.data.Participant;
import de.kjgstbarbara.services.GameEvaluationService;
import de.kjgstbarbara.services.GameGroupService;
import de.kjgstbarbara.services.ParticipantService;
import de.kjgstbarbara.service.PersonsRepository;
import de.kjgstbarbara.views.MainNavigationView;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;

@PermitAll
@Route(value = "gameevaluation/:game-evaluation/participant/:participant", layout = MainNavigationView.class)
public class ParticipantView extends Composite<VerticalLayout> implements BeforeEnterObserver {

    private final Grid<Game> gameGrid = new Grid<>();
    private final H3 title = new H3();

    private GameEvaluation gameEvaluation;
    private Participant participant;

    private final Person principal;

    public ParticipantView(PersonsRepository personsRepository, AuthenticationContext authenticationContext) {
        this.principal = authenticationContext.getAuthenticatedUser(UserDetails.class)
                .flatMap(userDetails -> personsRepository.findByUsername(userDetails.getUsername()))
                .orElse(null);
        if (principal == null) {
            authenticationContext.logout();
        }
        getContent().setSizeFull();

        HorizontalLayout header = new HorizontalLayout();
        header.setAlignItems(FlexComponent.Alignment.CENTER);

        Button back = new Button();
        back.setAriaLabel("Vorherige Seite");
        back.addClickListener(event -> UI.getCurrent().navigate(ScoreboardView.class, new RouteParameters(new RouteParam("game-evaluation", this.gameEvaluation.getId()))));
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
            this.participant.setName(editNameField.getValue());
            this.participant = this.participantService.update(this.participant);
            this.title.setText(this.participant.getName());
            this.title.setVisible(true);
            editNameField.setVisible(false);
            rename.setVisible(true);
            editNameDoneButton.setVisible(false);
        });

        getContent().add(header);

        gameGrid.setSelectionMode(Grid.SelectionMode.NONE);
        gameGrid.setHeightFull();
        gameGrid.setWidthFull();
        gameGrid.addColumn(Game::getName);
        gameGrid.addColumn(game -> game.getPointsOf(this.participant) + " Pkt").setFlexGrow(1);
        getContent().add(gameGrid);
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
        this.participant = beforeEnterEvent.getRouteParameters().get("participant").map(Long::valueOf).flatMap(participantService::get).orElse(null);
        if(participant == null) {
            beforeEnterEvent.rerouteTo("gameevaluation/" + this.gameEvaluation.getId() + "/participants");
            return;
        }
        this.title.setText(this.participant.getName());
        gameGrid.setItems(this.gameEvaluation.getGames());
    }
}