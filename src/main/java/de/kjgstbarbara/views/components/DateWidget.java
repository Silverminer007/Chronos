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
import com.vaadin.flow.component.details.DetailsVariant;
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
import de.kjgstbarbara.service.BoardsRepository;
import de.kjgstbarbara.service.DateRepository;
import de.kjgstbarbara.service.FeedbackRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.function.Supplier;

public class DateWidget extends ClosableDialog {

    private final FeedbackRepository feedbackRepository;
    private final DateRepository dateRepository;
    private final BoardsRepository boardsRepository;
    private final Date date;
    private final Person person;
    private boolean changedCalendar = false;

    public DateWidget(Date date, FeedbackRepository feedbackRepository, DateRepository dateRepository, BoardsRepository boardsRepository, Person person) {
        super();
        this.feedbackRepository = feedbackRepository;
        this.dateRepository = dateRepository;
        this.boardsRepository = boardsRepository;
        this.date = date;
        this.person = person;
        this.setTitle(header());
        this.update();
    }

    private Component header() {
        HorizontalLayout header = new HorizontalLayout();
        header.setAlignItems(FlexComponent.Alignment.CENTER);

        header.add(new H3(date.getTitle()));

        Button edit = new Button(VaadinIcon.PENCIL.create());
        edit.setVisible(date.getBoard().getAdmins().contains(person));
        edit.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        edit.addClickListener(event -> {
            new EditDateDialog(date, person, boardsRepository, dateRepository).open();
            this.changedCalendar = true;
        });
        header.add(edit);

        Button delete = new Button(VaadinIcon.TRASH.create());
        delete.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
        delete.setVisible(date.getBoard().getAdmins().contains(person));
        delete.addClickListener(event -> {
            ConfirmDialog confirmDelete = new ConfirmDialog(
                    "Bist du sicher, dass du diesen Termin löschen möchtest?",
                    date.getTitle() + " - Das löschen kann nicht Rückgängig gemacht werden",
                    "Ja, löschen",
                    e -> {
                        for (Feedback f : feedbackRepository.findAll()) {
                            if (f.getKey().getDate().getId() == date.getId()) {
                                feedbackRepository.delete(f);
                            }
                        }
                        dateRepository.delete(date);
                        this.changedCalendar = true;
                        this.close();
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
        Feedback feedback = feedbackRepository.findById(Feedback.Key.create(this.person, date)).orElse(Feedback.create(this.person, date, null));
        Feedback.Status status = feedback.getStatus();

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
            feedback.setStatus(Feedback.Status.IN);
            feedbackRepository.save(feedback);
            this.changedCalendar = true;
            this.update();
        });
        Button decline = new Button("Bin raus");
        decline.addClickListener(event -> {
            feedback.setStatus(Feedback.Status.OUT);
            feedbackRepository.save(feedback);
            this.changedCalendar = true;
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

    private HorizontalLayout createFeedbackWidget() {
        HorizontalLayout feedbacks = new HorizontalLayout();
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
            ClosableDialog dialog = new ClosableDialog(new H3(date.getTitle()));

            NativeLabel time = new NativeLabel((date.getStart().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT))));
            dialog.add(time, new Paragraph());

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
                    horizontalLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
                    horizontalLayout.setAlignItems(FlexComponent.Alignment.CENTER);
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
            furtherElements.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
            Button remindAll = new Button("Alle Erinnern");
            remindAll.addClickListener(e ->
                    Notification.show("Diese Funktion wurde noch nicht programmiert").addThemeVariants(NotificationVariant.LUMO_ERROR));
            remindAll.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
            if (date.isPollRunning() && date.getBoard().getAdmins().contains(this.person)) {
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

    @Override
    public void close() {
        super.close();
        if(changedCalendar) {
            UI.getCurrent().getPage().reload();
        }
    }
}
