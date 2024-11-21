package de.kjgstbarbara.views.date;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.accordion.AccordionPanel;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.kjgstbarbara.IcsHelper;
import de.kjgstbarbara.Result;
import de.kjgstbarbara.Utility;
import de.kjgstbarbara.components.*;
import de.kjgstbarbara.data.Date;
import de.kjgstbarbara.data.Feedback;
import de.kjgstbarbara.data.Person;
import de.kjgstbarbara.data.PollReminder;
import de.kjgstbarbara.messaging.MessageSender;
import de.kjgstbarbara.messaging.Messages;
import de.kjgstbarbara.service.*;
import de.kjgstbarbara.views.MainNavigationView;
import de.kjgstbarbara.views.date.calendar.CalendarListView;
import de.kjgstbarbara.views.date.calendar.CalendarPageView;
import de.kjgstbarbara.views.date.calendar.CalendarView;
import jakarta.annotation.security.PermitAll;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.vaadin.olli.FileDownloadWrapper;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

@PermitAll
@Route(value = "date/:date", layout = MainNavigationView.class)
public class DateView extends VerticalLayout implements BeforeEnterObserver {
    private static final Logger LOGGER = LogManager.getLogger(DateView.class);
    private final DateRepository dateRepository;
    private final FeedbackRepository feedbackRepository;
    private final GroupRepository groupRepository;
    private final OrganisationRepository organisationRepository;

    private final Person person;
    private final PollReminderRepository pollReminderRepository;

    private Date date;

    public DateView(PersonsService personsService, DatesService datesService, FeedbackService feedbackService, PollReminderRepository pollReminderRepository, GroupService groupService, OrganisationService organisationService) {
        this.dateRepository = datesService.getDateRepository();
        this.feedbackRepository = feedbackService.getFeedbackRepository();
        this.groupRepository = groupService.getGroupRepository();
        this.organisationRepository = organisationService.getOrganisationRepository();
        this.person = Utility.getAuthenticatedUser(personsService.getPersonsRepository()).orElse(null);
        this.setMaxWidth("600px");
        this.pollReminderRepository = pollReminderRepository;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        if (person == null) {
            return;
        }
        this.date = beforeEnterEvent.getRouteParameters().get("date").map(Long::valueOf).flatMap(dateRepository::findById).orElse(null);
        if (date == null || !date.getGroup().getMembers().contains(person)) {
            beforeEnterEvent.rerouteTo("");
            return;
        }
        this.removeAll();
        this.setPadding(false);
        this.setSpacing(false);
        this.setSizeFull();

        this.add(createHeader());

        Scroller dateInformationScroller = new Scroller(createDateInformation());
        dateInformationScroller.setSizeFull();
        this.add(dateInformationScroller);

        if (date.isPollRunning() && LocalDateTime.now(ZoneOffset.UTC).isBefore(date.getStartAtTimezone(ZoneOffset.UTC))) {
            this.add(createFooter());
        }
    }

    private HorizontalLayout createHeader() {
        HorizontalLayout header = new HorizontalLayout();
        header.setPadding(true);
        header.setSpacing(true);
        header.setWidthFull();
        header.setJustifyContentMode(JustifyContentMode.END);
        header.addClassNames(LumoUtility.Background.PRIMARY);

        HorizontalLayout leftHeader = new HorizontalLayout();
        leftHeader.setWidthFull();
        leftHeader.setAlignItems(Alignment.CENTER);

        Button back = new Button();
        back.addThemeVariants(ButtonVariant.LUMO_CONTRAST, ButtonVariant.LUMO_TERTIARY_INLINE);
        back.setAriaLabel("Vorherige Seite");
        back.addClickListener(event -> this.back());
        back.setIcon(VaadinIcon.ARROW_LEFT.create());
        leftHeader.add(back);

        Icon circleIcon = VaadinIcon.CIRCLE.create();
        circleIcon.setColor(date.getGroup().getColor());
        circleIcon.setSize("10px");
        leftHeader.add(circleIcon);

        H4 dateName = new H4(date.getTitle());
        leftHeader.add(dateName);

        header.add(leftHeader);

        HorizontalLayout rightHeader = new HorizontalLayout();
        rightHeader.setSpacing(false);
        rightHeader.setAlignItems(Alignment.CENTER);

        rightHeader.add(createDownloadIcsButton());
        if (this.date.getGroup().getAdmins().contains(this.person)) {
            rightHeader.add(createEditButton());
            rightHeader.add(createManageDateMenu());
        }

        header.add(rightHeader);

        return header;
    }

    private Component createEditButton() {
        Button edit = new Button(VaadinIcon.PENCIL.create());
        edit.addThemeVariants(ButtonVariant.LUMO_CONTRAST, ButtonVariant.LUMO_TERTIARY);
        edit.addClickListener(event -> {
            ClosableDialog selectEditModeDialog = new ClosableDialog("Bearbeitungsmodus (1/2)");

            RadioButtonGroup<EditMode> selectEditMode = new RadioButtonGroup<>();
            selectEditMode.setItems(EditMode.values());
            selectEditMode.setItemLabelGenerator(editMode -> switch (editMode) {
                case SINGLE -> "Nur diesen Termin bearbeiten";
                case FUTURE -> "Diesen und alle zukünftigen bearbeiten";
                case ALL -> "Alle Termine der Serie bearbeiten";
            });

            selectEditModeDialog.add(selectEditMode);

            Dialog.DialogFooter footer = selectEditModeDialog.getFooter();

            HorizontalLayout footerLayout = new HorizontalLayout();
            footerLayout.setWidthFull();
            footerLayout.setAlignItems(Alignment.CENTER);
            footerLayout.setJustifyContentMode(JustifyContentMode.BETWEEN);

            Button cancel = new Button("Abbrechen");
            cancel.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
            cancel.addClickListener(e -> selectEditModeDialog.close());
            footerLayout.add(cancel);

            Button continueButton = new Button("Weiter");
            continueButton.setEnabled(false);
            continueButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            continueButton.addClickShortcut(Key.ENTER);
            continueButton.addClickListener(e -> {
                selectEditModeDialog.close();
                createEditDialog(selectEditMode.getValue());
            });
            selectEditMode.addValueChangeListener(e -> continueButton.setEnabled(true));
            footerLayout.add(continueButton);

            footer.add(footerLayout);

            if (this.date.getLinkedTo() < 0) {
                createEditDialog(EditMode.SINGLE);
            } else {
                selectEditModeDialog.open();
            }
        });
        return edit;
    }

    private void createEditDialog(EditMode editMode) {
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
        binder.forField(startPicker).withConverter(new TimeZoneConverter(this.person.getTimezone())).bind(Date::getStart, Date::setStart);
        formLayout.add(startPicker);

        DateTimePicker endPicker = new DateTimePicker("Ende");
        binder.forField(endPicker).withConverter(new TimeZoneConverter(this.person.getTimezone())).bind(Date::getEnd, Date::setEnd);
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
                Date oldDate = new Date(date);
                binder.writeBean(date);

                long minutesStartChanged = ChronoUnit.MINUTES.between(oldDate.getStart(), date.getStart());
                long minutesEndChanged = ChronoUnit.MINUTES.between(oldDate.getEnd(), date.getEnd());

                dateRepository.save(date);
                if (!EditMode.SINGLE.equals(editMode)) {
                    for (Date d : dateRepository.findByLinkedTo(this.date.getLinkedTo())) {
                        if (d.getId() == date.getId()) {
                            continue;
                        }
                        if (!EditMode.FUTURE.equals(editMode) || d.getStart().isAfter(this.date.getStart())) {
                            if (!oldDate.getTitle().equals(date.getTitle())) {
                                d.setTitle(date.getTitle());
                            }
                            if (!oldDate.getVenue().equals(date.getVenue())) {
                                d.setVenue(date.getVenue());
                            }
                            if (!oldDate.getNotes().equals(date.getNotes())) {
                                d.setNotes(date.getNotes());
                            }
                            d.setStart(d.getStart().plusMinutes(minutesStartChanged));
                            d.setEnd(d.getEnd().plusMinutes(minutesEndChanged));
                            dateRepository.save(d);
                        }
                    }
                }
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

    private void back() {
        long page = (switch (this.person.getCalendarLayout()) {
            case MONTH ->
                    new CalendarPageView.Month(dateRepository, groupRepository, organisationRepository, this.person);
            case YEAR ->
                    new CalendarPageView.Year(dateRepository, groupRepository, organisationRepository, this.person);
            case LIST_NEXT -> new CalendarListView.Amount(dateRepository, feedbackRepository, this.person);
            default -> new CalendarListView.Month(dateRepository, feedbackRepository, this.person);
        }).getPage(this.date, "");
        UI.getCurrent().navigate(CalendarView.class, new RouteParameters(new RouteParam("page", page)));
    }

    private Component createDownloadIcsButton() {
        Button downloadIcs = new Button(VaadinIcon.DOWNLOAD.create());
        downloadIcs.addThemeVariants(ButtonVariant.LUMO_CONTRAST, ButtonVariant.LUMO_TERTIARY);

        FileDownloadWrapper downloadWrapper = new FileDownloadWrapper(
                new StreamResource(date.getTitle().toLowerCase(Locale.ROOT) + ".ics",
                        () ->
                                new ByteArrayInputStream(
                                        IcsHelper.writeDateToIcs(this.date)
                                                .getBytes(StandardCharsets.UTF_8))));
        downloadWrapper.getElement().setAttribute("download", true);
        downloadWrapper.wrapComponent(downloadIcs);
        return downloadWrapper;
    }

    private Component createManageDateMenu() {
        MenuBar menuBar = new MenuBar();
        Button menuButton = new Button(VaadinIcon.BULLETS.create());
        menuButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_CONTRAST);
        menuBar.addThemeVariants(MenuBarVariant.LUMO_TERTIARY, MenuBarVariant.LUMO_CONTRAST);
        MenuItem menuItem = menuBar.addItem(VaadinIcon.BULLETS.create());
        SubMenu subMenu = menuItem.getSubMenu();

        if (this.date.getStart().isAfter(LocalDateTime.now(ZoneOffset.UTC))) {
            if (this.date.getDateCancelled() == null) {
                subMenu.addItem("Erinnerung verschicken...", this::remindAll).addClassName(LumoUtility.TextColor.PRIMARY_CONTRAST);
            }
            if (this.date.isPollRunning()) {
                subMenu.addItem("Abstimmung beenden", this::stopPoll).addClassName(LumoUtility.TextColor.WARNING);
            }
            if (this.date.getDateCancelled() == null) {
                subMenu.addItem("Termin absagen", this::cancelForAll).addClassName(LumoUtility.TextColor.WARNING);
            }
        }
        subMenu.addItem("Termin löschen", this::deleteDate).addClassName(LumoUtility.TextColor.ERROR);

        return menuBar;
    }

    private void cancelForAll(ClickEvent<MenuItem> event) {
        ConfirmDialog confirmCancelDate = new ConfirmDialog();
        confirmCancelDate.setHeader("Bist du sicher, dass du diesen Termin für alle absagen möchtest?");
        confirmCancelDate.setText("Alle Personen werden informiert, dass du diesen Termin abgesagt hast");
        confirmCancelDate.setCancelable(true);
        confirmCancelDate.setCancelText("Nicht absagen");
        confirmCancelDate.setConfirmText("Absagen");
        confirmCancelDate.addConfirmListener(confirmEvent -> {
            this.date.setDateCancelled(LocalDate.now(ZoneOffset.UTC));
            dateRepository.save(this.date);
            for (Person p : this.date.getGroup().getMembers()) {
                new MessageSender(p).date(this.date).person(p).feedback(this.date.getStatusFor(p)).send(Messages.DATE_CANCELLED);
            }
            UI.getCurrent().navigate(DateView.class);
        });
        confirmCancelDate.open();
    }

    private void deleteDate(ClickEvent<MenuItem> event) {
        if (this.date.getLinkedTo() > 0) {// Serientermin
            createConfirmRepitingDateDeletionDialog().open();
        } else { // Einzeltermin
            createConfirmSingleDateDeletionDialog().open();
        }
    }

    private Dialog createConfirmRepitingDateDeletionDialog() {
        Dialog dialog = new Dialog();
        dialog.setCloseOnEsc(true);
        dialog.setCloseOnOutsideClick(true);

        RadioButtonGroup<EditMode> deleteOptions = new RadioButtonGroup<>();
        deleteOptions.setItems(EditMode.values());
        deleteOptions.setItemLabelGenerator(option -> switch (option) {
            case SINGLE -> "Nur diesen Termin";
            case FUTURE -> "Diesen und alle zukünftigen";
            case ALL -> "Die ganze Serie";
        });
        dialog.add(deleteOptions);

        Dialog.DialogFooter footer = dialog.getFooter();
        HorizontalLayout footerLayout = new HorizontalLayout();
        footerLayout.setJustifyContentMode(JustifyContentMode.BETWEEN);
        footerLayout.setAlignItems(Alignment.CENTER);
        footerLayout.setWidthFull();

        Button cancel = new Button("Nichts löschen");
        cancel.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        cancel.addClickListener(event -> dialog.close());
        footerLayout.add(cancel);

        Button delete = new Button("Löschen");
        delete.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
        delete.setEnabled(false);
        delete.addClickShortcut(Key.ENTER);
        delete.addClickListener(event -> {
            if (EditMode.SINGLE.equals(deleteOptions.getValue())) {
                deleteDate(this.date, true);
                this.back();
                Notification.show("\"" + this.date.getTitle() + "\" wurde gelöscht");
            } else {
                int deletedDates = 1;
                for (Date d : dateRepository.findByLinkedTo(this.date.getLinkedTo())) {
                    if (d.getId() == date.getId()) {
                        continue;
                    }
                    if (!EditMode.FUTURE.equals(deleteOptions.getValue()) || d.getStart().isAfter(this.date.getStart())) {
                        this.deleteDate(d, false);
                        deletedDates++;
                    }
                }
                deleteDate(this.date, false);
                this.back();
                Notification.show(deletedDates + " Termine wurde(n) gelöscht");
            }
            dialog.close();
        });
        deleteOptions.addValueChangeListener(event -> delete.setEnabled(true));
        footerLayout.add(delete);

        footer.add(footerLayout);

        return dialog;
    }

    private ConfirmDialog createConfirmSingleDateDeletionDialog() {
        ConfirmDialog confirmDeleteDialog = new ConfirmDialog();
        confirmDeleteDialog.setHeader("Bist du sicher, dass du diesen Termin löschen möchtest?");
        confirmDeleteDialog.setText("Der Termin wird für alle Personen gelöscht und kann nicht wiederhergestellt werden");
        confirmDeleteDialog.setCancelable(true);
        confirmDeleteDialog.setCloseOnEsc(true);
        confirmDeleteDialog.setCancelText("Nicht löschen");
        confirmDeleteDialog.setConfirmText("Löschen");
        confirmDeleteDialog.addConfirmListener(confirmEvent -> {
            deleteDate(this.date, true);
            this.back();
            Notification.show("\"" + this.date.getTitle() + "\" wurde gelöscht");
        });
        return confirmDeleteDialog;
    }

    private void deleteDate(Date date, boolean reLink) {
        if (reLink && date.getLinkedTo() == date.getId()) {
            List<Date> linkedDates = dateRepository.findByLinkedTo(date.getId()).stream().sorted().toList();
            if (!linkedDates.isEmpty()) {
                long newLinkedTo = linkedDates.get(0).getId();
                for (Date d : linkedDates) {
                    d.setLinkedTo(newLinkedTo);
                    dateRepository.save(d);
                }
            }
        }
        dateRepository.delete(date);
    }

    private void remindAll(ClickEvent<MenuItem> event) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Terminerinnerungen");
        Button planPollReminder = new Button("Erinnerung Planen");
        planPollReminder.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        planPollReminder.addClickListener(e -> this.planPollReminder());
        dialog.add(planPollReminder);
        dialog.add(new Hr());
        Button remindNow = new Button("Jetzt erinnern");
        remindNow.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        remindNow.addClickListener(e -> {
            Result result = Result.success();
            for (Person member : date.getGroup().getMembers()) {
                result = result.and(new MessageSender(member).date(this.date).person(member).send(Messages.DATE_POLL));
            }
            if (result.isSuccess()) {
                Notification.show("Die Abfrage wurde erfolgreich verschickt")
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } else {
                LOGGER.error(result.getErrorMessage());
                Notification.show(result.getErrorMessage())
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
            dialog.close();
        });
        dialog.add(remindNow);
        dialog.open();
    }

    private void planPollReminder() {
        PollReminder pollReminder = pollReminderRepository.findByDate(this.date).orElseGet(() -> {
            PollReminder pR = new PollReminder();
            pR.setDate(this.date);
            pR.setPollStarter(this.person);
            pR.setPollIntervalStart(this.date.getStart().minusWeeks(1));
            pR.setPollIntervalEnd(this.date.getStart());
            return pR;
        });
        Binder<PollReminder> binder = new Binder<>(PollReminder.class);

        ClosableDialog planPollDialog = new ClosableDialog("Erinnerungen planen");

        VerticalLayout contentLayout = new VerticalLayout();
        contentLayout.setSizeFull();

        NativeLabel helpText = new NativeLabel("Alle Personen die noch nicht abgestimmt haben, bekommen in diesem Zeitraum in immer kürzer werdenden Abständen Aufforderungen für diesen Termin abzustimmen");

        DateTimePicker intervalStartPicker = new DateTimePicker();
        binder.forField(intervalStartPicker)
                .withValidator((Validator<LocalDateTime>) (localDateTime, valueContext) ->
                        this.date.getStart().isAfter(localDateTime) ?
                                ValidationResult.ok()
                                : ValidationResult.error("Die Erinnerungen können nur vor dem Start des Termins verschickt werden"))
                .bind(PollReminder::getPollIntervalStart, PollReminder::setPollIntervalStart);

        DateTimePicker intervalEndPicker = new DateTimePicker();
        binder.forField(intervalEndPicker)
                .withValidator((Validator<LocalDateTime>) (localDateTime, valueContext) ->
                        !this.date.getStart().isBefore(localDateTime)
                                ? localDateTime.isAfter(intervalStartPicker.getValue())
                                ? ValidationResult.ok()
                                : ValidationResult.error("Das Ende des Erinnerungszeitraums sollte nach seinem Ende liegen")
                                : ValidationResult.error("Die Erinnerungen können nur vor dem Start des Termins verschickt werden"))
                .bind(PollReminder::getPollIntervalEnd, PollReminder::setPollIntervalEnd);

        contentLayout.add(helpText, intervalStartPicker, intervalEndPicker);

        planPollDialog.add(contentLayout);

        Button planPollReminder = new Button("Speichern");
        planPollReminder.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        planPollReminder.addClickListener(event -> {
            try {
                binder.writeBean(pollReminder);
                pollReminder.setPollStarter(this.person);
                this.pollReminderRepository.save(pollReminder);
                planPollDialog.close();
                Notification.show("Erinnerung für den " + intervalStartPicker.getValue().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) + " geplant");
            } catch (ValidationException e) {
                LOGGER.error(e.getMessage());
            }
        });
        planPollDialog.getFooter().add(planPollReminder);

        planPollDialog.open();
        binder.readBean(pollReminder);
    }

    private void stopPoll(ClickEvent<MenuItem> event) {
        ConfirmDialog confirmDialog = new ConfirmDialog(
                "Bist du sicher, dass du die Abfrage beenden möchtest?",
                "Das kann nicht mehr Rückgängig gemacht werden",
                "Abfrage beenden",
                (confirmEvent) -> {
                    date.setPollRunning(false);
                    dateRepository.save(date);
                    UI.getCurrent().getPage().getHistory().go(0);
                    Notification.show("Abstimmung gestoppt");
                });
        confirmDialog.setCancelable(true);
        confirmDialog.open();
    }

    private Component createDateInformation() {
        VerticalLayout dateInformation = new VerticalLayout();
        dateInformation.setHeightFull();
        dateInformation.setPadding(true);
        dateInformation.setSpacing(true);

        if (this.date.getDateCancelled() != null) {
            H5 cancelledWarning = new H5(String.format("Dieser Termin wurde am %s abgesagt", this.date.getDateCancelled().format(DateTimeFormatter.ofPattern("d MMM yyyy"))));
            cancelledWarning.addClassName(LumoUtility.TextColor.ERROR);
            dateInformation.add(cancelledWarning);
        }

        dateInformation.add(new NativeLabel("Start: " + date.getStartAtTimezone(this.person.getTimezone()).format(DateTimeFormatter.ofPattern("dd.MM.yyyy   HH:mm")) + " Uhr"));
        dateInformation.add(new NativeLabel("Ende: " + date.getEndAtTimezone(this.person.getTimezone()).format(DateTimeFormatter.ofPattern("dd.MM.yyyy   HH:mm")) + " Uhr"));

        boolean hasVenue = !(date.getVenue() == null || date.getVenue().isBlank());
        NativeLabel venue = new NativeLabel((hasVenue ? date.getVenue() : "kein Ort angegeben"));
        if (!hasVenue) {
            venue.addClassName(LumoUtility.TextColor.DISABLED);
        }
        dateInformation.add(new HorizontalLayout(new NativeLabel("Ort: "), venue));

        dateInformation.add(new NativeLabel("Notizen:"));
        VerticalLayout notes = new VerticalLayout();
        notes.setWidthFull();
        notes.addClassName(LumoUtility.Background.CONTRAST_5);
        notes.add(new NativeLabel(date.getNotes()));
        dateInformation.add(notes);

        dateInformation.add(new H6(date.getGroup().getOrganisation().getName() + " - " + date.getGroup().getName()));

        FeedbackAvatars committed = new FeedbackAvatars(Feedback.Status.COMMITTED, date);
        FeedbackAvatars cancelled = new FeedbackAvatars(Feedback.Status.CANCELLED, date);
        FeedbackAvatars noFeedback = new FeedbackAvatars(Feedback.Status.NONE, date);
        dateInformation.add(committed, cancelled, noFeedback);

        dateInformation.add(createPersonalFeedback());
        dateInformation.add(createPersonsInformation());

        return dateInformation;
    }

    private Component createPersonalFeedback() {
        Feedback.Status status = date.getStatusFor(person);
        Icon icon = switch (status) {
            case COMMITTED -> VaadinIcon.THUMBS_UP.create();
            case CANCELLED -> VaadinIcon.THUMBS_DOWN.create();
            case NONE -> VaadinIcon.QUESTION_CIRCLE.create();
        };
        icon.setColor(switch (status) {
            case COMMITTED -> "#00ff00";
            case CANCELLED -> "#ff0000";
            case NONE -> "#00ffff";
        });
        icon.setSize("20px");
        HorizontalLayout personalFeedback = new HorizontalLayout(createPersonalFeedbackLabel(status), icon);
        personalFeedback.setAlignItems(Alignment.CENTER);
        return personalFeedback;
    }

    private NativeLabel createPersonalFeedbackLabel(Feedback.Status status) {
        NativeLabel personalFeedback = new NativeLabel(switch (status) {
            case COMMITTED -> "Du hast zu diesem Termin zugesagt";
            case CANCELLED -> "Du hast zu diesen Termin abgesagt";
            case NONE -> "Du hast keine Rückmeldung zu diesem Termin gegeben";
        });
        personalFeedback.addClassName(switch (status) {
            case COMMITTED -> LumoUtility.TextColor.SUCCESS;
            case CANCELLED -> LumoUtility.TextColor.ERROR;
            case NONE -> LumoUtility.TextColor.TERTIARY;// TODO Own Color classes
        });
        return personalFeedback;
    }

    private Component createPersonsInformation() {
        Accordion informationAccordion = new Accordion();
        informationAccordion.setWidthFull();
        AccordionPanel personalInformationPanel = new AccordionPanel("Informationen (" + date.getInformation().size() + ")");

        for (Date.Information information : date.getInformation().stream().sorted().toList()) {
            VerticalLayout informationLayout = new VerticalLayout();
            informationLayout.setWidthFull();
            informationLayout.setSpacing(false);
            informationLayout.addClassName(LumoUtility.Background.CONTRAST_5);

            informationLayout.add(new HorizontalLayout(
                    createBadge(information.getInformationSender().getName()),
                    createBadge(information.getInformationTime().format(DateTimeFormatter.ofPattern("EEE, dd MMM yy")))
            ));
            informationLayout.add(new NativeLabel(information.getInformationText()));
            personalInformationPanel.add(informationLayout);
        }

        informationAccordion.add(personalInformationPanel);

        return informationAccordion;
    }

    private Span createBadge(String value) {
        Span badge = new Span(value);
        badge.getElement().getThemeList().add("badge small contrast");
        badge.getStyle().set("margin-inline-start", "var(--lumo-space-xs)");
        return badge;
    }

    private Component createFooter() {
        HorizontalLayout actions = new HorizontalLayout();
        actions.setWidthFull();
        actions.setSpacing(true);
        actions.setPadding(true);
        actions.setJustifyContentMode(JustifyContentMode.CENTER);

        Feedback.Status status = date.getStatusFor(person);
        Button commit = new FeedbackButton(Feedback.Status.COMMITTED, true, !Feedback.Status.COMMITTED.equals(status));
        Button info = createInfoButton();
        Button cancel = new FeedbackButton(Feedback.Status.CANCELLED, true, !Feedback.Status.CANCELLED.equals(status));
        actions.add(commit, info, cancel);

        commit.addClickListener(event -> {
            Feedback feedback = new Feedback(person, Feedback.Status.COMMITTED);
            date.addFeedback(feedback);
            this.feedbackRepository.save(feedback);
            this.date = dateRepository.save(date);
            UI.getCurrent().navigate(DateView.class, new RouteParameters(new RouteParam("date", this.date.getId())));
        });
        cancel.addClickListener(event -> {
            Feedback feedback = new Feedback(person, Feedback.Status.CANCELLED);
            date.addFeedback(feedback);
            this.feedbackRepository.save(feedback);
            this.date = dateRepository.save(date);
            UI.getCurrent().navigate(DateView.class, new RouteParameters(new RouteParam("date", this.date.getId())));
        });

        return actions;
    }

    private Button createInfoButton() {
        Button infoButton = new Button("Info", VaadinIcon.INFO_CIRCLE_O.create());
        infoButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);

        infoButton.addClickListener(event -> {
            ClosableDialog submitInfoDialog = new ClosableDialog("Anmerkung schreiben");
            submitInfoDialog.setMaxWidth("400px");

            TextField infoText = new TextField("Anmerkung");
            infoText.focus();
            infoText.setWidthFull();
            submitInfoDialog.add(infoText);

            submitInfoDialog.getFooter().add(new DialogFooter(submitInfoDialog::close, () -> {
                if (!infoText.getValue().isBlank()) {
                    Date.Information information = new Date.Information();
                    information.setInformationSender(this.person);
                    information.setInformationTime(LocalDateTime.now(ZoneOffset.UTC));
                    information.setInformationText(infoText.getValue());
                    date.getInformation().add(information);
                    this.date = dateRepository.save(date);
                    UI.getCurrent().navigate(DateView.class, new RouteParameters(new RouteParam("date", date.getId())));
                    Notification.show("Anmerkung wurde gespeichert");
                }
                submitInfoDialog.close();
            }, "Abschicken"));
            submitInfoDialog.open();
        });
        return infoButton;
    }

    public enum EditMode {
        SINGLE, FUTURE, ALL
    }
}
