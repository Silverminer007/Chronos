package de.kjgstbarbara.views;

import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;
import de.kjgstbarbara.data.Date;
import de.kjgstbarbara.data.Feedback;
import de.kjgstbarbara.data.Person;
import de.kjgstbarbara.service.*;
import de.kjgstbarbara.views.components.DateWidget;
import de.kjgstbarbara.views.nav.MainNavigationView;
import github.tobsef.vaadin.paperfab.SpeedDial;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Route(value = "", layout = MainNavigationView.class)
@PageTitle("Meine Termine")
@PermitAll
public class StartView extends VerticalLayout implements AfterNavigationObserver {
    private final PersonsRepository personsRepository;
    private final DateRepository dateRepository;
    private final BoardsRepository boardsRepository;
    private final FeedbackRepository feedbackRepository;

    private final Person person;
    private final Grid<Date> grid = new Grid<>(Date.class, false);

    public StartView(PersonsService personsService, DatesService datesService, BoardsService boardsService, FeedbackService feedbackService, AuthenticationContext authenticationContext) {
        this.personsRepository = personsService.getPersonsRepository();
        this.dateRepository = datesService.getDateRepository();
        this.boardsRepository = boardsService.getBoardsRepository();
        this.feedbackRepository = feedbackService.getFeedbackRepository();
        this.person = authenticationContext.getAuthenticatedUser(UserDetails.class)
                .flatMap(userDetails -> personsRepository.findByUsername(userDetails.getUsername()))
                .orElse(null);
        if(person == null) {
            authenticationContext.logout();
        }

        this.setHeightFull();
        grid.addClassName("styling");
        grid.setHeightFull();
        grid.addComponentColumn(date -> new DateWidget(date, feedbackRepository, this.person));
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_ROW_STRIPES);
        grid.setPartNameGenerator(date -> {
            Feedback.Status status = feedbackRepository.findById(Feedback.Key.create(this.person, date)).map(Feedback::getStatus).orElse(null);
            if (Feedback.Status.IN.equals(status)) {
                return "green";
            }
            if (Feedback.Status.OUT.equals(status)) {
                return "red";
            }
            if (Feedback.Status.DONTKNOW.equals(status)) {
                return "yellow";
            }
            return null;
        });
        add(grid);
    }

    private List<Date> getDates() {
        List<Date> dates = new ArrayList<>();
        boardsRepository.findByPerson(this.person).stream().map(dateRepository::findByBoard).forEach(dates::addAll);
        return dates.stream().distinct().sorted().toList();
    }

    @Override
    public void afterNavigation(AfterNavigationEvent afterNavigationEvent) {
        List<Date> dates = getDates();
        grid.setItems(dates);
        for(int i = 0; i < dates.size(); i++) {
            if(dates.get(i).getStart().isAfter(LocalDateTime.now())) {
                grid.scrollToIndex(i);
                break;
            }
        }
    }
}