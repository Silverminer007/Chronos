package de.kjgstbarbara.views.date;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.avatar.AvatarGroup;
import com.vaadin.flow.component.avatar.AvatarGroupVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;
import com.vaadin.flow.spring.security.AuthenticationContext;
import de.kjgstbarbara.FrontendUtils;
import de.kjgstbarbara.data.*;
import de.kjgstbarbara.service.*;
import de.kjgstbarbara.views.CalendarView;
import de.kjgstbarbara.views.MainNavigationView;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

@PermitAll
@Route(value = "date/:date", layout = MainNavigationView.class)
public class DateView extends VerticalLayout implements BeforeEnterObserver {
    private final DateRepository dateRepository;
    private final FeedbackRepository feedbackRepository;

    private Date date;
    private final Person person;

    public DateView(PersonsService personsService, FeedbackService feedbackService, DatesService datesService, AuthenticationContext authenticationContext) {
        this.dateRepository = datesService.getDateRepository();
        this.feedbackRepository = feedbackService.getFeedbackRepository();
        person = authenticationContext.getAuthenticatedUser(UserDetails.class)
                .flatMap(userDetails -> personsService.getPersonsRepository().findByUsername(userDetails.getUsername()))
                .orElse(null);
        if (person == null) {
            authenticationContext.logout();
        }
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        this.date = beforeEnterEvent.getRouteParameters().get("date").map(Long::valueOf).flatMap(dateRepository::findById).orElse(null);
        if (date == null) {
            beforeEnterEvent.rerouteTo("");
            return;
        }
        this.removeAll();
        this.setPadding(true);

        HorizontalLayout header = new HorizontalLayout();
        header.setJustifyContentMode(JustifyContentMode.CENTER);
        Button back = new Button();
        back.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        back.setAriaLabel("Vorherige Seite");
        back.addClickListener(event -> UI.getCurrent().navigate(CalendarView.class, new RouteParameters(new RouteParam("week", date.getStart().toLocalDate().toString()), new RouteParam("layout", CalendarView.Layout.LIST.toString()))));
        back.setIcon(VaadinIcon.ARROW_LEFT.create());
        header.add(back);
        H3 dateName = new H3(date.getTitle());
        header.add(dateName);
        this.add(header);// TODO Edit and delete

        Feedback.Status status = date.getStatusFor(person);

        NativeLabel time = new NativeLabel((date.getStart().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM))) +
                (date.getEnd() == null ? "" : (" - " + date.getEnd().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)))));
        this.add(time);

        HorizontalLayout committedWithLabel = new HorizontalLayout();
        committedWithLabel.add(new NativeLabel("Zusagen:"));
        committedWithLabel.setJustifyContentMode(JustifyContentMode.START);
        committedWithLabel.setAlignItems(Alignment.CENTER);
        committedWithLabel.setWidthFull();

        AvatarGroup committedAvatars = new AvatarGroup();
        committedAvatars.setMaxItemsVisible(7);
        committedAvatars.addThemeVariants(AvatarGroupVariant.LUMO_SMALL);
        committedAvatars.setItems(FrontendUtils.getAvatars(date, Feedback.Status.COMMITTED));
        committedWithLabel.add(committedAvatars);

        committedWithLabel.addClickListener(event -> UI.getCurrent().navigate(DateFeedbackOverviewView.class, new RouteParameters(new RouteParam("date", date.getId()))));
        this.add(committedWithLabel);

        HorizontalLayout cancelledWithLabel = new HorizontalLayout();
        cancelledWithLabel.add(new NativeLabel("Absagen:"));
        cancelledWithLabel.setJustifyContentMode(JustifyContentMode.CENTER);
        cancelledWithLabel.setAlignItems(Alignment.CENTER);
        cancelledWithLabel.setWidthFull();

        AvatarGroup cancelledAvatars = new AvatarGroup();
        cancelledAvatars.setMaxItemsVisible(7);
        cancelledAvatars.addThemeVariants(AvatarGroupVariant.LUMO_SMALL);
        cancelledAvatars.setItems(FrontendUtils.getAvatars(date, Feedback.Status.CANCELLED));
        cancelledWithLabel.add(cancelledAvatars);

        cancelledWithLabel.addClickListener(event -> UI.getCurrent().navigate(DateFeedbackOverviewView.class, new RouteParameters(new RouteParam("date", date.getId()))));
        this.add(cancelledWithLabel);

        boolean pollRunning = date.isPollRunning() && LocalDateTime.now().isBefore(date.getStart());

        HorizontalLayout footer = new HorizontalLayout();
        footer.setWidthFull();
        footer.setVisible(pollRunning);

        Button commit = new Button("Bin dabei");
        commit.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        commit.setEnabled(!Feedback.Status.COMMITTED.equals(status));
        commit.setWidth("50%");
        footer.add(commit);

        Button cancel = new Button("Bin raus");
        cancel.addThemeVariants(ButtonVariant.LUMO_ERROR);
        cancel.setEnabled(!Feedback.Status.CANCELLED.equals(status));
        cancel.setWidth("50%");
        footer.add(cancel);
        this.add(footer);

        NativeLabel pollStoppedLabel = new NativeLabel("Die Abfrage läuft nicht mehr");
        pollStoppedLabel.setVisible(!pollRunning);
        this.add(pollStoppedLabel);

        commit.addClickListener(event -> {
            Feedback feedback = Feedback.create(person, Feedback.Status.COMMITTED);
            feedbackRepository.save(feedback);
            date.addFeedback(feedback);
            this.date = dateRepository.save(date);
            committedAvatars.setItems(FrontendUtils.getAvatars(date, Feedback.Status.COMMITTED));
            cancelledAvatars.setItems(FrontendUtils.getAvatars(date, Feedback.Status.CANCELLED));
            commit.setEnabled(false);
            cancel.setEnabled(true);
        });
        cancel.addClickListener(event -> {
            Feedback feedback = Feedback.create(person, Feedback.Status.CANCELLED);
            feedbackRepository.save(feedback);
            date.addFeedback(feedback);
            this.date = dateRepository.save(date);
            committedAvatars.setItems(FrontendUtils.getAvatars(date, Feedback.Status.COMMITTED));
            cancelledAvatars.setItems(FrontendUtils.getAvatars(date, Feedback.Status.CANCELLED));
            commit.setEnabled(true);
            cancel.setEnabled(false);
        });
    }
}
