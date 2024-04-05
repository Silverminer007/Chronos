package de.kjgstbarbara.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.LocalDateTimeRenderer;
import com.vaadin.flow.router.*;
import com.vaadin.flow.spring.security.AuthenticationContext;
import de.kjgstbarbara.data.*;
import de.kjgstbarbara.service.*;
import de.kjgstbarbara.views.nav.MyDatesNavigationView;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.List;

@Route(value = "my-dates")
@PermitAll
public class MyDatesView extends VerticalLayout implements AfterNavigationObserver {
    private final PersonsRepository personsRepository;
    private final DateRepository dateRepository;
    private final FeedbackRepository feedbackRepository;

    private final Person person;
    private final Grid<Date> grid = new Grid<>(Date.class, false);

    public MyDatesView(PersonsService personsService, DatesService datesService, FeedbackService feedbackService, AuthenticationContext authenticationContext) {
        this.personsRepository = personsService.getPersonsRepository();
        this.dateRepository = datesService.getDateRepository();
        this.feedbackRepository = feedbackService.getFeedbackRepository();
        this.person = authenticationContext.getAuthenticatedUser(UserDetails.class)
                .flatMap(userDetails -> personsRepository.findByUsername(userDetails.getUsername()))
                .orElse(null);
        if(person == null) {
            authenticationContext.logout();
        }

        grid.addClassName("styling");
        grid.addColumn(date -> date.getBoard().getOrganisation().getName()).setHeader("Organisation");
        grid.addColumn(date -> date.getBoard().getTitle()).setHeader("Board");
        grid.addColumn("title").setHeader("Termin")
                .setAutoWidth(true).setResizable(true)
                .setPartNameGenerator(date -> "font-weight-bold");
        grid.addColumn("start")
                .setRenderer(new LocalDateTimeRenderer<>(Date::getStart, "dd.MM.YYYY HH:mm"))
                .setHeader("Datum").setAutoWidth(true).setResizable(true);
        grid.addComponentColumn(date -> {
            HorizontalLayout horizontalLayout = new HorizontalLayout();
            horizontalLayout.setSpacing(false);
            Feedback feedback = feedbackRepository.findById(Feedback.Key.create(this.person, date)).orElse(Feedback.create(this.person, date, null));
            Feedback.Status status = feedback.getStatus();
            Button in = new Button("Zusagen");
            in.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
            in.setEnabled(!Feedback.Status.IN.equals(status));
            Button out = new Button("Absagen");
            out.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
            out.setEnabled(!Feedback.Status.OUT.equals(status));
            Button dontknow = new Button("Weiß nicht");
            dontknow.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_CONTRAST);
            dontknow.setEnabled(!Feedback.Status.DONTKNOW.equals(status));
            in.addClickListener(event -> {
                in.setEnabled(false);
                out.setEnabled(true);
                dontknow.setEnabled(true);
                feedback.setStatus(Feedback.Status.IN);
                feedbackRepository.save(feedback);
            });
            out.addClickListener(event -> {
                in.setEnabled(true);
                out.setEnabled(false);
                dontknow.setEnabled(true);
                feedback.setStatus(Feedback.Status.OUT);
                feedbackRepository.save(feedback);
            });
            dontknow.addClickListener(event -> {
                in.setEnabled(true);
                out.setEnabled(true);
                dontknow.setEnabled(false);
                feedback.setStatus(Feedback.Status.DONTKNOW);
                feedbackRepository.save(feedback);
            });
            horizontalLayout.add(in, out, dontknow);
            return horizontalLayout;
        }).setAutoWidth(true);
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
        List<Date> dates = new ArrayList<>(dateRepository.hasAuthorityOn(Role.Type.MEMBER, this.person));
        return dates.stream().distinct().sorted().toList();
    }

    @Override
    public void afterNavigation(AfterNavigationEvent afterNavigationEvent) {
        grid.setItems(getDates());
    }
}