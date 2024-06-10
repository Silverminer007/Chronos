package de.kjgstbarbara.views;

import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.accordion.AccordionPanel;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.avatar.AvatarGroup;
import com.vaadin.flow.component.avatar.AvatarGroupVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.details.DetailsVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;
import de.kjgstbarbara.FriendlyError;
import de.kjgstbarbara.data.*;
import de.kjgstbarbara.service.*;
import de.kjgstbarbara.views.components.ClosableDialog;
import de.kjgstbarbara.views.nav.MainNavigationView;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.function.Supplier;

@PermitAll
@Route(value = "date/:dateID", layout = MainNavigationView.class)
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

    private Dialog feedbackOverviewDialog(Date date, Person person) {
        ClosableDialog feedbackOverview = new ClosableDialog();
        feedbackOverview.setTitle(new H3(date.getTitle()));

        NativeLabel time = new NativeLabel((date.getStart().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT))));
        feedbackOverview.add(time, new Paragraph());

        Accordion accordion = new Accordion();

        AccordionPanel confirmedUsers = new AccordionPanel();
        confirmedUsers.addThemeVariants(DetailsVariant.FILLED, DetailsVariant.REVERSE);
        confirmedUsers.setEnabled(false);
        accordion.add(confirmedUsers);

        AccordionPanel declinedUsers = new AccordionPanel();
        declinedUsers.addThemeVariants(DetailsVariant.FILLED, DetailsVariant.REVERSE);
        declinedUsers.setEnabled(false);
        accordion.add(declinedUsers);

        AccordionPanel noFeedback = new AccordionPanel();
        noFeedback.addThemeVariants(DetailsVariant.FILLED, DetailsVariant.REVERSE);
        noFeedback.setEnabled(false);
        accordion.add(noFeedback);

        int confirmedAmount = 0;
        int declinedAmount = 0;
        int noFeedbackAmount = 0;
        for (Person p : date.getGroup().getMembers()) {
            Feedback.Status status = date.getStatusFor(p);
            Supplier<HorizontalLayout> personEntry = () -> {
                HorizontalLayout horizontalLayout = new HorizontalLayout();
                Avatar avatar = p.getAvatar();
                H4 label = new H4(avatar.getName());
                horizontalLayout.add(avatar, label);
                horizontalLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
                horizontalLayout.setAlignItems(FlexComponent.Alignment.CENTER);
                return horizontalLayout;
            };
            if (Feedback.Status.COMMITTED.equals(status)) {
                confirmedUsers.add(personEntry.get());
                confirmedUsers.add(new Paragraph());
                confirmedUsers.setEnabled(true);
                confirmedAmount++;
            } else if (Feedback.Status.CANCELLED.equals(status)) {
                declinedUsers.add(personEntry.get());
                declinedUsers.add(new Paragraph());
                declinedUsers.setEnabled(true);
                declinedAmount++;
            } else {
                noFeedback.add(personEntry.get());
                noFeedback.add(new Paragraph());
                noFeedback.setEnabled(true);
                noFeedbackAmount++;
            }
        }
        confirmedUsers.setSummaryText("Zusagen (" + confirmedAmount + ")");
        declinedUsers.setSummaryText("Absagen (" + declinedAmount + ")");
        noFeedback.setSummaryText("Keine Rückmeldung (" + noFeedbackAmount + ")");
        feedbackOverview.add(accordion);

        feedbackOverview.add(new Paragraph());

        if (date.getPollScheduledFor() != null && date.isPollRunning()) {
            feedbackOverview.add(new NativeLabel("Erinnerung geplant für den " + date.getPollScheduledFor().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT))));
            feedbackOverview.add(new Paragraph());
        }

        HorizontalLayout furtherElements = new HorizontalLayout();
        furtherElements.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        Button remindAll = new Button("Alle Erinnern");
        remindAll.addClickListener(e -> remindAllDialog(date, person).open());
        remindAll.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        if (date.isPollRunning() && date.getGroup().getAdmins().contains(person)) {
            furtherElements.add(remindAll);
        }

        Button stopPoll = new Button("Abfrage stoppen");
        stopPoll.addClickListener(e -> {
            ConfirmDialog confirmDialog = new ConfirmDialog(
                    "Bist du sicher, dass du die Abfrage beenden möchtest?",
                    "Das kann nicht mehr Rückgängig gemacht werden",
                    "Abfrage beenden",
                    (confirmEvent) -> {
                        date.setPollRunning(false);
                        dateRepository.save(date);
                        stopPoll.setVisible(false);
                        remindAll.setVisible(false);
                    });
            confirmDialog.setCancelable(true);
            confirmDialog.open();
        });
        stopPoll.addThemeVariants(ButtonVariant.LUMO_ERROR);
        if (date.isPollRunning() && date.getGroup().getAdmins().contains(person)) {
            furtherElements.add(stopPoll);
        }

        if (!date.isPollRunning()) {
            furtherElements.add(new H4("Die Abfrage wurde beendet"));
        }

        feedbackOverview.add(furtherElements);

        Button history = new Button("Historie");
        history.setWidthFull();
        history.addClickListener(e -> historyDialog(date).open());
        feedbackOverview.add(history);
        return feedbackOverview;
    }

    private Dialog remindAllDialog(Date date, Person person) {
        ClosableDialog remindAllDialog = new ClosableDialog(new H3("Erinnerung verschicken"));
        remindAllDialog.add(new Hr());
        remindAllDialog.add(new H4("Erinnerung planen"));
        if (date.getPollScheduledFor() != null) {
            remindAllDialog.add(new NativeLabel("Aktuell geplant für den " + date.getPollScheduledFor().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT))));
            remindAllDialog.add(new Paragraph());
        }
        DatePicker datePicker = new DatePicker();
        datePicker.setValue(LocalDate.now().plusDays(1));
        remindAllDialog.add(datePicker);
        Button scheduleReminder = new Button("Erinnerung planen");
        scheduleReminder.addClickListener(e -> {
            if (datePicker.getValue() == null) {
                datePicker.setInvalid(true);
                datePicker.setErrorMessage("Kein Datum ausgewählt");
            } else if (datePicker.getValue().isBefore(LocalDate.now())) {
                datePicker.setInvalid(true);
                datePicker.setErrorMessage("Das Datum muss in der Zukunft liegen");
            } else if (datePicker.getValue().isAfter(date.getStart().toLocalDate())) {
                datePicker.setInvalid(true);
                datePicker.setErrorMessage("Die Abfrage sollte vor beginn des Termins gesendet werden");
            } else {
                date.setPollScheduledFor(datePicker.getValue());
                this.dateRepository.save(date);
                feedbackOverviewDialog(date, person);
                remindAllDialog.close();
            }
        });
        remindAllDialog.add(scheduleReminder);
        remindAllDialog.add(new Hr());
        Button remindNow = new Button("Jetzt erinnern");
        remindNow.addClickListener(e -> {
            try {
                date.getGroup().getOrganisation().sendDatePollToAll(date);
                Notification.show("Die Abfrage wurde erfolgreich verschickt")
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                remindAllDialog.close();
            } catch (FriendlyError ex) {
                Notification.show("Die Abfrage konnte nicht an alle verschickt werden")
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        remindAllDialog.add(remindNow);
        return remindAllDialog;
    }

    private Dialog historyDialog(Date date) {
        ClosableDialog historyDialog = new ClosableDialog("Historie");
        historyDialog.setWidthFull();

        Grid<Feedback> feedbackHistory = new Grid<>();
        feedbackHistory.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_NO_ROW_BORDERS, GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_WRAP_CELL_CONTENT);

        feedbackHistory.addComponentColumn(feedback -> feedback.getPerson().getAvatar()).setFlexGrow(0);
        feedbackHistory.addColumn(feedback -> feedback.getPerson().getName());
        feedbackHistory.addColumn(feedback -> feedback.getTimeStamp().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)));
        feedbackHistory.addColumn(feedback ->
                switch (feedback.getStatus()) {
                    case COMMITTED -> "Zugesagt";
                    case CANCELLED -> "Abgesagt";
                    default -> "Feedback gelöscht";
                });
        feedbackHistory.addComponentColumn(feedback ->
                switch (feedback.getStatus()) {
                    case COMMITTED -> VaadinIcon.CHECK.create();
                    case CANCELLED -> VaadinIcon.CLOSE.create();
                    default -> new Div();
                });

        feedbackHistory.setItems(date.getFeedbackList().stream().sorted().toList());

        historyDialog.add(feedbackHistory);

        return historyDialog;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        this.date = beforeEnterEvent.getRouteParameters().get("dateID").map(Long::valueOf).flatMap(dateRepository::findById).orElse(null);
        if(date == null) {
            beforeEnterEvent.rerouteTo("calendar");
        } else {
            this.removeAll();
            this.setPadding(true);

            this.add(new H3(date.getTitle()));// TODO Edit and delete

            Feedback.Status status = date.getStatusFor(person);

            NativeLabel time = new NativeLabel((date.getStart().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM))) +
                    (date.getEnd() == null ? "" : (" - " + date.getEnd().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)))));
            this.add(time);


            HorizontalLayout feedbacks = new HorizontalLayout();
            feedbacks.setWidthFull();

            HorizontalLayout committedWithLabel = new HorizontalLayout();
            committedWithLabel.add(new NativeLabel("Zusagen:"));
            committedWithLabel.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
            committedWithLabel.setAlignItems(FlexComponent.Alignment.CENTER);
            committedWithLabel.setWidth("50%");

            AvatarGroup committedAvatars = new AvatarGroup();
            committedAvatars.setMaxItemsVisible(4);
            committedAvatars.addThemeVariants(AvatarGroupVariant.LUMO_SMALL);
            committedAvatars.setItems(date.getAvatars(Feedback.Status.COMMITTED));
            committedWithLabel.add(committedAvatars);

            HorizontalLayout cancelledWithLabel = new HorizontalLayout();
            cancelledWithLabel.add(new NativeLabel("Absagen:"));
            cancelledWithLabel.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
            cancelledWithLabel.setAlignItems(FlexComponent.Alignment.CENTER);
            cancelledWithLabel.setWidth("50%");

            AvatarGroup cancelledAvatars = new AvatarGroup();
            cancelledAvatars.setMaxItemsVisible(4);
            cancelledAvatars.addThemeVariants(AvatarGroupVariant.LUMO_SMALL);
            cancelledAvatars.setItems(date.getAvatars(Feedback.Status.CANCELLED));
            cancelledWithLabel.add(cancelledAvatars);

            feedbacks.add(committedWithLabel, cancelledWithLabel);
            feedbacks.addClickListener(event -> feedbackOverviewDialog(date, person).open());
            feedbacks.setHeight(committedAvatars.getHeight());
            this.add(feedbacks);

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

            commit.addClickListener(event -> {// TODO Direkt das Feedback zu ändern wirft eine Fehlermeldung, erst beim zweiten öffnen möglich
                Feedback feedback = Feedback.create(person, Feedback.Status.COMMITTED);
                feedbackRepository.save(feedback);
                date.addFeedback(feedback);
                dateRepository.save(date);
                committedAvatars.setItems(date.getAvatars(Feedback.Status.COMMITTED));
                cancelledAvatars.setItems(date.getAvatars(Feedback.Status.CANCELLED));
                commit.setEnabled(false);
                cancel.setEnabled(true);
            });
            cancel.addClickListener(event -> {
                Feedback feedback = Feedback.create(person, Feedback.Status.CANCELLED);
                feedbackRepository.save(feedback);
                date.addFeedback(feedback);
                dateRepository.save(date);
                committedAvatars.setItems(date.getAvatars(Feedback.Status.COMMITTED));
                cancelledAvatars.setItems(date.getAvatars(Feedback.Status.CANCELLED));
                commit.setEnabled(true);
                cancel.setEnabled(false);
            });
        }
    }
}
