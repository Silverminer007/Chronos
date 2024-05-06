package de.kjgstbarbara.views.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
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
import de.kjgstbarbara.data.Date;
import de.kjgstbarbara.data.Feedback;
import de.kjgstbarbara.data.Person;
import de.kjgstbarbara.messaging.SenderUtils;
import de.kjgstbarbara.service.GroupRepository;
import de.kjgstbarbara.service.DateRepository;
import de.kjgstbarbara.service.FeedbackRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.function.Supplier;

public class DateWidget extends ClosableDialog {

    private final FeedbackRepository feedbackRepository;
    private final DateRepository dateRepository;
    private final GroupRepository groupRepository;
    private final SenderUtils senderUtils;
    private final Date date;
    private final Person person;

    public DateWidget(Date date, FeedbackRepository feedbackRepository, DateRepository dateRepository, GroupRepository groupRepository, SenderUtils senderUtils, Person person) {
        super();
        this.feedbackRepository = feedbackRepository;
        this.dateRepository = dateRepository;
        this.groupRepository = groupRepository;
        this.date = date;
        this.senderUtils = senderUtils;
        this.person = person;
        this.setTitle(header());
        this.update();
    }

    private Component header() {
        HorizontalLayout header = new HorizontalLayout();
        header.setAlignItems(FlexComponent.Alignment.CENTER);

        header.add(new H3(date.getTitle()));

        Button edit = new Button(VaadinIcon.PENCIL.create());
        edit.setVisible(date.getGroup().getAdmins().contains(person));
        edit.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        edit.addClickListener(event -> {
            new EditDateDialog(date, person, groupRepository, dateRepository).open();
        });
        header.add(edit);

        Button delete = new Button(VaadinIcon.TRASH.create());
        delete.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
        delete.setVisible(date.getGroup().getAdmins().contains(person));
        delete.addClickListener(event -> {
            ConfirmDialog confirmDelete = new ConfirmDialog(
                    "Bist du sicher, dass du diesen Termin löschen möchtest?",
                    date.getTitle() + " - Das löschen kann nicht Rückgängig gemacht werden",
                    "Ja, löschen",
                    e -> {
                        for (Feedback f : date.getFeedbackList()) {
                            feedbackRepository.delete(f);
                        }
                        dateRepository.delete(date);
                    }
            );
            confirmDelete.setCancelable(true);
            confirmDelete.setCancelText("Abbruch");
            confirmDelete.setCloseOnEsc(true);
            confirmDelete.open();
        });

        header.add(delete);
        return header;
    }

    private final VerticalLayout content = new VerticalLayout();

    private void update() {
        Feedback.Status status = this.date.getStatusFor(this.person);

        content.removeAll();
        content.setPadding(true);

        NativeLabel time = new NativeLabel((date.getStart().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM))) +
                (date.getEnd() == null ? "" : (" - " + date.getEnd().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)))));
        content.add(time);


        HorizontalLayout feedbacks = createFeedbackWidget();
        content.add(feedbacks);

        HorizontalLayout footer = new HorizontalLayout();
        Button confirm = new Button("Bin dabei");
        confirm.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        confirm.setEnabled(!Feedback.Status.IN.equals(status));
        confirm.setWidth("50%");
        confirm.addClickListener(event -> {
            Feedback feedback = Feedback.create(this.person, Feedback.Status.IN);
            feedbackRepository.save(feedback);
            date.addFeedback(feedback);
            dateRepository.save(date);
            this.update();
        });
        Button decline = new Button("Bin raus");
        decline.addClickListener(event -> {
            Feedback feedback = Feedback.create(this.person, Feedback.Status.OUT);
            feedbackRepository.save(feedback);
            date.addFeedback(feedback);
            dateRepository.save(date);
            this.update();
        });
        decline.addThemeVariants(ButtonVariant.LUMO_ERROR);
        decline.setEnabled(!Feedback.Status.OUT.equals(status));
        decline.setWidth("50%");
        footer.add(confirm, decline);
        footer.setWidthFull();
        if (date.isPollRunning() && LocalDateTime.now().isBefore(date.getStart())) {
            content.add(footer);
        } else {
            content.add(new NativeLabel("Die Abfrage läuft nicht mehr"));
        }

        this.add(content);
    }

    private final HorizontalLayout feedbacks = new HorizontalLayout();

    private HorizontalLayout createFeedbackWidget() {
        feedbacks.removeAll();
        AvatarGroup confirmed = new AvatarGroup();
        confirmed.setMaxItemsVisible(4);
        confirmed.addThemeVariants(AvatarGroupVariant.LUMO_SMALL);
        HorizontalLayout confirmedLayout = new HorizontalLayout(new NativeLabel("Zusagen:"), confirmed);
        confirmedLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
        confirmedLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        confirmedLayout.setWidth("50%");
        AvatarGroup declined = new AvatarGroup();
        declined.setMaxItemsVisible(4);
        declined.addThemeVariants(AvatarGroupVariant.LUMO_SMALL);
        HorizontalLayout declinedLayout = new HorizontalLayout(new NativeLabel("Absagen:"), declined);
        declinedLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        declinedLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        declinedLayout.setWidth("50%");
        for (Person p : date.getGroup().getMembers()) {
            Feedback.Status status = date.getStatusFor(p);
            if (Feedback.Status.IN.equals(status)) {
                confirmed.add(p.getAvatarGroupItem());
            } else if (Feedback.Status.OUT.equals(status)) {
                declined.add(p.getAvatarGroupItem());
            }
        }
        feedbacks.setWidthFull();
        feedbacks.add(confirmedLayout, declinedLayout);
        feedbacks.addClickListener(event -> feedbackOverviewDialog().open());
        feedbacks.setHeight(confirmed.getHeight());
        return feedbacks;
    }

    private final ClosableDialog feedbackOverview = new ClosableDialog();

    private Dialog feedbackOverviewDialog() {
        feedbackOverview.removeAll();
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
            Feedback.Status status = this.date.getStatusFor(p);
            Supplier<HorizontalLayout> personEntry = () -> {
                HorizontalLayout horizontalLayout = new HorizontalLayout();
                Avatar avatar = p.getAvatar();
                H4 label = new H4(avatar.getName());
                horizontalLayout.add(avatar, label);
                horizontalLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
                horizontalLayout.setAlignItems(FlexComponent.Alignment.CENTER);
                return horizontalLayout;
            };
            if (Feedback.Status.IN.equals(status)) {
                confirmedUsers.add(personEntry.get());
                confirmedUsers.add(new Paragraph());
                confirmedUsers.setEnabled(true);
                confirmedAmount++;
            } else if (Feedback.Status.OUT.equals(status)) {
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
        remindAll.addClickListener(e -> remindAllDialog().open());
        remindAll.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        if (date.isPollRunning() && date.getGroup().getAdmins().contains(this.person)) {
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
        if (date.isPollRunning() && date.getGroup().getAdmins().contains(this.person)) {
            furtherElements.add(stopPoll);
        }

        if (!date.isPollRunning()) {
            furtherElements.add(new H4("Die Abfrage wurde beendet"));
        }

        feedbackOverview.add(furtherElements);

        Button history = new Button("Historie");
        history.setWidthFull();
        history.addClickListener(e -> historyDialog().open());
        feedbackOverview.add(history);
        return feedbackOverview;
    }

    private final ClosableDialog remindAllDialog = new ClosableDialog(new H3("Erinnerung verschicken"));

    private Dialog remindAllDialog() {
        remindAllDialog.removeAll();
        remindAllDialog.add(new Hr());
        remindAllDialog.add(new H4("Erinnerung planen"));
        if (date.getPollScheduledFor() != null) {
            remindAllDialog.add(new NativeLabel("Aktuell geplant für den " + date.getPollScheduledFor().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT))));
            remindAllDialog.add(new Paragraph());
        }
        DatePicker datePicker = new DatePicker();
        datePicker.setValue(LocalDate.now().plusDays(1));
        remindAllDialog.add(datePicker);
        Button scheduleReminder = getScheduleReminder(datePicker);
        remindAllDialog.add(scheduleReminder);
        remindAllDialog.add(new Hr());
        Button remindNow = getRemindNow();
        remindAllDialog.add(remindNow);
        return remindAllDialog;
    }

    private Button getRemindNow() {
        Button remindNow = new Button("Jetzt erinnern");
        remindNow.addClickListener(e -> {
            if (!senderUtils.sendDatePoll(this.date, true)) {
                Notification.show("Die Abfrage konnte nicht an alle verschickt werden")
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            } else {
                Notification.show("Die Abfrage wurde erfolgreich verschickt")
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                remindAllDialog.close();
            }
        });
        return remindNow;
    }

    private Button getScheduleReminder(DatePicker datePicker) {
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
                this.date.setPollScheduledFor(datePicker.getValue());
                this.dateRepository.save(date);
                feedbackOverviewDialog();
                remindAllDialog.close();
            }
        });
        return scheduleReminder;
    }

    private final ClosableDialog historyDialog = new ClosableDialog("Historie");

    private Dialog historyDialog() {
        historyDialog.removeAll();
        historyDialog.setWidthFull();

        Grid<Feedback> feedbackHistory = new Grid<>();
        feedbackHistory.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_NO_ROW_BORDERS, GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_WRAP_CELL_CONTENT);

        feedbackHistory.addComponentColumn(feedback -> feedback.getPerson().getAvatar()).setFlexGrow(0);
        feedbackHistory.addColumn(feedback -> feedback.getPerson().getName());
        feedbackHistory.addColumn(feedback -> feedback.getTimeStamp().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)));
        feedbackHistory.addColumn(feedback ->
                switch (feedback.getStatus()) {
                    case IN -> "Zugesagt";
                    case OUT -> "Abgesagt";
                    default -> "Feedback gelöscht";
                });
        feedbackHistory.addComponentColumn(feedback ->
                switch (feedback.getStatus()) {
                    case IN -> VaadinIcon.CHECK.create();
                    case OUT -> VaadinIcon.CLOSE.create();
                    default -> new Div();
                });

        feedbackHistory.setItems(this.date.getFeedbackList().stream().sorted().toList());

        historyDialog.add(feedbackHistory);

        return historyDialog;
    }
}
