package de.kjgstbarbara.views.components;

import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.accordion.AccordionPanel;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.avatar.AvatarGroup;
import com.vaadin.flow.component.avatar.AvatarGroupVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.details.DetailsVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import de.kjgstbarbara.data.Date;
import de.kjgstbarbara.data.Feedback;
import de.kjgstbarbara.data.Person;
import de.kjgstbarbara.service.DateRepository;
import de.kjgstbarbara.service.FeedbackRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class DateWidget extends VerticalLayout {

    private final FeedbackRepository feedbackRepository;
    private final DateRepository dateRepository;
    private final Date date;
    private final Person person;
    private boolean expanded;

    public DateWidget(Date date, FeedbackRepository feedbackRepository, DateRepository dateRepository, Person person) {
        this.feedbackRepository = feedbackRepository;
        this.dateRepository = dateRepository;
        this.date = date;
        this.person = person;
        this.update();
    }

    private void update() {
        boolean inFuture = LocalDateTime.now().isBefore(date.getStart());
        Feedback feedback = feedbackRepository.findById(Feedback.Key.create(this.person, date)).orElse(Feedback.create(this.person, date, null));
        Feedback.Status status = feedback.getStatus();
        this.removeAll();
        HorizontalLayout primaryLine = new HorizontalLayout();
        primaryLine.setWidthFull();
        primaryLine.addClickListener(event -> this.toggle());
        primaryLine.setJustifyContentMode(JustifyContentMode.CENTER);
        primaryLine.setAlignItems(Alignment.CENTER);

        H3 startDate = new H3(date.getStart().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)));
        startDate.addClickListener(event -> this.toggle());
        if (!expanded) {
            primaryLine.add(startDate);
        }

        H3 title = new H3();
        title.setText("(" + date.getBoard().getTitle() + ") " + date.getTitle());
        primaryLine.add(title);

        HorizontalLayout rightSidePrimaryLine = new HorizontalLayout();

        Button edit = new Button(VaadinIcon.PENCIL.create());
        edit.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        //edit.setVisible(date.getBoard().getAdmins().contains(this.person));
        edit.setVisible(false);//TODO Implement
        rightSidePrimaryLine.add(edit);
        rightSidePrimaryLine.setWidthFull();
        rightSidePrimaryLine.setVerticalComponentAlignment(Alignment.START);
        rightSidePrimaryLine.setAlignItems(Alignment.END);
        rightSidePrimaryLine.setJustifyContentMode(JustifyContentMode.END);
        primaryLine.add(rightSidePrimaryLine);
        this.add(primaryLine);
        if (this.expanded) {
            H4 time = new H4((date.getStart().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM))) +
                    (date.getEnd() == null ? "" : (" - " + date.getEnd().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)))));
            this.add(time);


            HorizontalLayout feedbacks = createFeedbackWidget();
            this.add(feedbacks);

            HorizontalLayout footer = new HorizontalLayout();
            Button confirm = new Button("Zusagen");
            confirm.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
            confirm.setEnabled(!Feedback.Status.IN.equals(status));
            confirm.setWidth("50%");
            confirm.addClickListener(event -> {
                feedback.setStatus(Feedback.Status.IN);
                feedbackRepository.save(feedback);
                this.update();
            });
            Button decline = new Button("Absagen");
            decline.addClickListener(event -> {
                feedback.setStatus(Feedback.Status.OUT);
                feedbackRepository.save(feedback);
                this.update();
            });
            decline.addThemeVariants(ButtonVariant.LUMO_ERROR);
            decline.setEnabled(!Feedback.Status.OUT.equals(status));
            decline.setWidth("50%");
            footer.add(confirm, decline);
            footer.setWidthFull();
            if (date.isPollRunning() && inFuture) {
                this.add(footer);
            }
            this.setHeight(title.getHeight() + time.getHeight() + feedbacks.getHeight() + (date.isPollRunning() ? confirm.getHeight() : ""));
        } else {
            this.setHeight(title.getHeight());
        }
    }

    private HorizontalLayout createFeedbackWidget() {
        HorizontalLayout feedbacks = new HorizontalLayout();
        AvatarGroup confirmed = new AvatarGroup();
        confirmed.setMaxItemsVisible(4);
        confirmed.addThemeVariants(AvatarGroupVariant.LUMO_SMALL);
        HorizontalLayout confirmedLayout = new HorizontalLayout(new NativeLabel("Zusagen:"), confirmed);
        confirmedLayout.setJustifyContentMode(JustifyContentMode.START);
        confirmedLayout.setAlignItems(Alignment.CENTER);
        confirmedLayout.setWidth("50%");
        AvatarGroup declined = new AvatarGroup();
        declined.setMaxItemsVisible(4);
        declined.addThemeVariants(AvatarGroupVariant.LUMO_SMALL);
        HorizontalLayout declinedLayout = new HorizontalLayout(new NativeLabel("Absagen:"), declined);
        declinedLayout.setJustifyContentMode(JustifyContentMode.CENTER);
        declinedLayout.setAlignItems(Alignment.CENTER);
        declinedLayout.setWidth("50%");
        for (Person p : date.getBoard().getMembers()) {
            Feedback.Status status = feedbackRepository.findById(Feedback.Key.create(p, date)).map(Feedback::getStatus).orElse(null);
            if (Feedback.Status.IN.equals(status)) {
                confirmed.add(p.getAvatarGroupItem());
            } else if (Feedback.Status.OUT.equals(status)) {
                declined.add(p.getAvatarGroupItem());
            }
        }
        feedbacks.setWidthFull();
        feedbacks.add(confirmedLayout, declinedLayout);
        feedbacks.addClickListener(event -> {
            Dialog dialog = new Dialog("Rückmeldungen zu \"" + date.getTitle() + "\"");
            Accordion accordion = new Accordion();
            AccordionPanel confirmedUsers = new AccordionPanel("Zusagen (" + confirmed.getItems().size() + ")");
            confirmedUsers.addThemeVariants(DetailsVariant.FILLED, DetailsVariant.REVERSE);
            if (confirmed.getItems().isEmpty()) {
                confirmedUsers.setEnabled(false);
            }
            AccordionPanel declinedUsers = new AccordionPanel("Absagen (" + declined.getItems().size() + ")");
            declinedUsers.addThemeVariants(DetailsVariant.FILLED, DetailsVariant.REVERSE);
            if (declined.getItems().isEmpty()) {
                declinedUsers.setEnabled(false);
            }
            AccordionPanel noFeedback = new AccordionPanel("Keine Rückmeldung (" + (date.getBoard().getMembers().size() - confirmed.getItems().size() - declined.getItems().size()) + ")");
            noFeedback.addThemeVariants(DetailsVariant.FILLED, DetailsVariant.REVERSE);
            if (date.getBoard().getMembers().size() - confirmed.getItems().size() - declined.getItems().size() == 0) {
                noFeedback.setEnabled(false);
            }
            accordion.add(confirmedUsers);
            accordion.add(declinedUsers);
            accordion.add(noFeedback);
            for (Person p : date.getBoard().getMembers()) {
                Feedback.Status status = feedbackRepository.findById(Feedback.Key.create(p, date)).map(Feedback::getStatus).orElse(null);
                Supplier<HorizontalLayout> personEntry = () -> {
                    HorizontalLayout horizontalLayout = new HorizontalLayout();
                    Avatar avatar = p.getAvatar();
                    H4 label = new H4(avatar.getName());
                    horizontalLayout.add(avatar, label);
                    horizontalLayout.setJustifyContentMode(JustifyContentMode.START);
                    horizontalLayout.setAlignItems(Alignment.CENTER);
                    return horizontalLayout;
                };
                if (Feedback.Status.IN.equals(status)) {
                    confirmedUsers.add(personEntry.get());
                    confirmedUsers.add(new Paragraph());
                } else if (Feedback.Status.OUT.equals(status)) {
                    declinedUsers.add(personEntry.get());
                    declinedUsers.add(new Paragraph());
                } else {
                    noFeedback.add(personEntry.get());
                    noFeedback.add(new Paragraph());
                }
            }
            dialog.add(accordion);
            dialog.add(new Paragraph());
            HorizontalLayout furtherElements = new HorizontalLayout();
            furtherElements.setJustifyContentMode(JustifyContentMode.BETWEEN);
            Button remindAll = new Button("Alle Erinnern");
            remindAll.addClickListener(e -> {
                Notification.show("Diese Funktion wurde noch nicht programmiert").addThemeVariants(NotificationVariant.LUMO_ERROR);
            });
            remindAll.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
            if (date.isPollRunning() && date.getBoard().getAdmins().contains(this.person)) {
                furtherElements.add(remindAll);
            }
            Button stopPoll = new Button("Abfrage stoppen");
            stopPoll.addClickListener(e -> {
                ConfirmDialog confirmDialog = new ConfirmDialog("Bist du sicher, dass du die Abfrage beenden möchtest?", "Das kann nicht mehr Rückgängig gemacht werden", "Abfrage beenden", (confirmEvent) -> {
                    date.setPollRunning(false);
                    dateRepository.save(date);
                    stopPoll.setVisible(false);
                    remindAll.setVisible(false);
                });
                confirmDialog.setCancelable(true);
                confirmDialog.open();
            });
            stopPoll.addThemeVariants(ButtonVariant.LUMO_ERROR);
            if (date.isPollRunning() && date.getBoard().getAdmins().contains(this.person)) {
                furtherElements.add(stopPoll);
            }
            if (!date.isPollRunning()) {
                furtherElements.add(new H4("Die Abfrage wurde beendet"));
            }
            dialog.add(furtherElements);
            dialog.open();
        });
        feedbacks.setHeight(confirmed.getHeight());
        return feedbacks;
    }

    public void toggle() {
        this.expanded = !this.expanded;
        this.update();
    }
}
