package de.kjgstbarbara.views.date;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.avatar.AvatarGroup;
import com.vaadin.flow.component.avatar.AvatarGroupVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.router.*;
import com.vaadin.flow.spring.security.AuthenticationContext;
import de.kjgstbarbara.FrontendUtils;
import de.kjgstbarbara.components.ClosableDialog;
import de.kjgstbarbara.data.Date;
import de.kjgstbarbara.data.Feedback;
import de.kjgstbarbara.data.Person;
import de.kjgstbarbara.service.*;
import de.kjgstbarbara.views.CalendarView;
import de.kjgstbarbara.views.MainNavigationView;
import jakarta.annotation.security.PermitAll;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;

@PermitAll
@Route(value = "date/:date", layout = MainNavigationView.class)
public class DateView extends VerticalLayout implements BeforeEnterObserver {
    private static final Logger LOGGER = LogManager.getLogger();
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
        header.setAlignItems(Alignment.CENTER);
        Button back = new Button();
        back.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        back.setAriaLabel("Vorherige Seite");
        back.addClickListener(event -> UI.getCurrent().navigate(CalendarView.class, new RouteParameters(new RouteParam("week", date.getStart().toLocalDate().toString()), new RouteParam("layout", CalendarView.Layout.LIST.toString()))));
        back.setIcon(VaadinIcon.ARROW_LEFT.create());
        header.add(back);
        H3 dateName = new H3(date.getTitle());
        header.add(dateName);
        header.add(createEditButton());
        header.add(createDeleteButton());
        this.add(header);

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

    private Component createEditButton() {
        Button edit = new Button(VaadinIcon.PENCIL.create());
        edit.addThemeVariants(ButtonVariant.LUMO_CONTRAST, ButtonVariant.LUMO_TERTIARY);
        edit.addClickListener(event -> createEditDialog());
        return edit;
    }

    private void createEditDialog() {
        ClosableDialog editDateDialog = new ClosableDialog("Termin bearbeiten (2/2)");
        editDateDialog.setMaxWidth("600px");

        VerticalLayout formLayout = new VerticalLayout();
        formLayout.setSpacing(false);
        formLayout.setPadding(false);

        Binder<Date> binder = new Binder<>();

        TextField title = new TextField("Name");
        title.setWidthFull();
        binder.forField(title).bind(Date::getTitle, Date::setTitle);
        formLayout.add(title);

        DateTimePicker startPicker = new DateTimePicker("Beginn");
        binder.forField(startPicker).bind(Date::getStart, Date::setStart);
        formLayout.add(startPicker);

        DateTimePicker endPicker = new DateTimePicker("Ende");
        binder.forField(endPicker).bind(Date::getEnd, Date::setEnd);
        formLayout.add(endPicker);
        startPicker.addValueChangeListener(event -> {
            if (event.getValue() != null && (endPicker.getValue() == null || endPicker.getValue().isBefore(event.getValue()))) {
                endPicker.setValue(event.getOldValue() == null || endPicker.getValue() == null
                        ? event.getValue().plusHours(1)
                        : endPicker.getValue().plusMinutes(ChronoUnit.MINUTES.between(event.getOldValue(), endPicker.getValue()))
                );
            }
        });

        TextField venue = new TextField("Veranstaltungsort");
        venue.setWidthFull();
        binder.forField(venue).bind(Date::getVenue, Date::setVenue);
        formLayout.add(venue);

        TextArea notes = new TextArea("Notizen");
        notes.setWidthFull();
        binder.forField(notes).bind(Date::getNotes, Date::setNotes);
        formLayout.add(notes);

        editDateDialog.add(formLayout);

        Dialog.DialogFooter footer = editDateDialog.getFooter();

        HorizontalLayout footerLayout = new HorizontalLayout();
        footerLayout.setWidthFull();
        footerLayout.setJustifyContentMode(JustifyContentMode.BETWEEN);
        footerLayout.setAlignItems(Alignment.CENTER);

        Button cancel = new Button("Abbrechen");
        cancel.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        cancel.addClickListener(e -> editDateDialog.close());
        footerLayout.add(cancel);

        Button save = new Button("Speichern");
        save.addClickShortcut(Key.ENTER);
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        save.addClickListener(e -> {
            try {
                binder.writeBean(date);

                dateRepository.save(date);
                editDateDialog.close();
                UI.getCurrent().navigate(DateView.class, new RouteParameters(new RouteParam("date", date.getId())));
                Notification.show("Änderungen gespeichert");
            } catch (ValidationException ex) {
                LOGGER.error("Failed to edit date", ex);
            }
        });
        footerLayout.add(save);

        footer.add(footerLayout);

        binder.readBean(date);

        editDateDialog.open();
    }

    private Component createDeleteButton() {
        Button edit = new Button(VaadinIcon.TRASH.create());
        edit.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
        edit.addClickListener(event -> createConfirmDateDeletionDialog().open());
        return edit;
    }

    private ConfirmDialog createConfirmDateDeletionDialog() {
        ConfirmDialog confirmDeleteDialog = new ConfirmDialog();
        confirmDeleteDialog.setHeader("Bist du sicher, dass du diesen Termin löschen möchtest?");
        confirmDeleteDialog.setText("Der Termin wird für alle Personen gelöscht und kann nicht wiederhergestellt werden");
        confirmDeleteDialog.setCancelable(true);
        confirmDeleteDialog.setCloseOnEsc(true);
        confirmDeleteDialog.setCancelText("Nicht löschen");
        confirmDeleteDialog.setConfirmText("Löschen");
        confirmDeleteDialog.addConfirmListener(confirmEvent -> {
            dateRepository.delete(date);
            UI.getCurrent().navigate(CalendarView.class);
            Notification.show("\"" + this.date.getTitle() + "\" wurde gelöscht");
        });
        return confirmDeleteDialog;
    }
}
