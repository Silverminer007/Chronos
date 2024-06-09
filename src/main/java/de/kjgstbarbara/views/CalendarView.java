package de.kjgstbarbara.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.accordion.AccordionPanel;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.avatar.AvatarGroup;
import com.vaadin.flow.component.avatar.AvatarGroupVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.details.DetailsVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.theme.lumo.LumoIcon;
import de.kjgstbarbara.FriendlyError;
import de.kjgstbarbara.data.*;
import de.kjgstbarbara.service.*;
import de.kjgstbarbara.views.components.ClosableDialog;
import de.kjgstbarbara.views.components.NonNullValidator;
import de.kjgstbarbara.views.nav.MainNavigationView;
import jakarta.annotation.security.PermitAll;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.vaadin.stefan.fullcalendar.*;
import org.vaadin.stefan.fullcalendar.dataprovider.CallbackEntryProvider;
import org.vaadin.stefan.fullcalendar.dataprovider.EntryProvider;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

@Route(value = "calendar/:week?/:layout?", layout = MainNavigationView.class)
@RouteAlias(value = ":week?/:layout?", layout = MainNavigationView.class)
@PageTitle("Meine Termine")
@PermitAll
public class CalendarView extends VerticalLayout implements BeforeEnterObserver {
    private static final Logger LOGGER = LoggerFactory.getLogger(CalendarView.class);

    private final PersonsRepository personsRepository;
    private final OrganisationRepository organisationRepository;
    private final DateRepository dateRepository;
    private final GroupRepository groupRepository;
    private final FeedbackRepository feedbackRepository;

    private final Person person;

    private final FullCalendar fullCalendar = FullCalendarBuilder.create().withAutoBrowserLocale()/*.withAutoBrowserTimezone()*/.build();// Siehe EditDateDialog
    private final H4 selectedStartDateIndicator = new H4();
    private LocalDate selectedStartDate = LocalDate.now();
    private Layout selectedCalendarLayout = Layout.LIST;

    private final Button previous = new Button(LumoIcon.ARROW_LEFT.create());
    private final Button today = new Button(LumoIcon.CALENDAR.create());
    private final Button next = new Button(LumoIcon.ARROW_RIGHT.create());

    public CalendarView(PersonsService personsService, OrganisationService organisationService, DatesService datesService, GroupService groupService, FeedbackService feedbackService, AuthenticationContext authenticationContext) {
        this.personsRepository = personsService.getPersonsRepository();
        this.organisationRepository = organisationService.getOrganisationRepository();
        this.dateRepository = datesService.getDateRepository();
        this.groupRepository = groupService.getGroupRepository();
        this.feedbackRepository = feedbackService.getFeedbackRepository();
        this.person = authenticationContext.getAuthenticatedUser(UserDetails.class)
                .flatMap(userDetails -> personsRepository.findByUsername(userDetails.getUsername()))
                .orElse(null);
        if (person == null) {
            authenticationContext.logout();
        }

        this.setSizeFull();

        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);
        header.setAlignItems(Alignment.CENTER);

        MenuBar menu = new MenuBar();
        menu.addThemeVariants(MenuBarVariant.LUMO_TERTIARY_INLINE);

        Button calendarLayoutMenu = new Button(VaadinIcon.ELLIPSIS_DOTS_V.create());
        calendarLayoutMenu.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        MenuItem calendarLayout = menu.addItem(calendarLayoutMenu);
        SubMenu subMenu = calendarLayout.getSubMenu();
        for (Layout layout : Layout.values()) {
            Button changeLayout = new Button(layout.getDisplayName());
            changeLayout.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
            changeLayout.addClickListener(event -> UI.getCurrent().navigate(CalendarView.class,
                    new RouteParameters(new RouteParam("layout", layout.name()), new RouteParam("week", this.selectedStartDate.toString()))));
            subMenu.addItem(changeLayout);
        }
        header.add(menu);

        HorizontalLayout navigation = new HorizontalLayout();
        navigation.setAlignItems(Alignment.CENTER);
        navigation.setJustifyContentMode(JustifyContentMode.CENTER);
        navigation.add(previous);
        navigation.add(today);
        navigation.add(selectedStartDateIndicator);
        navigation.add(next);
        header.add(navigation);

        HorizontalLayout create = new HorizontalLayout();
        create.setAlignItems(Alignment.END);
        create.setJustifyContentMode(JustifyContentMode.END);
        Button createNew = new Button(VaadinIcon.PLUS.create());
        createNew.setTooltipText("Einen neuen Termin erstellen");
        createNew.setAriaLabel("Neuer Termin");
        createNew.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
        createNew.addClickListener(event -> createEditDateDialog(new Date()).open());
        create.add(createNew);
        header.add(create);

        this.add(header);

        fullCalendar.setSizeFull();
        fullCalendar.addThemeVariants(FullCalendarVariant.LUMO);
        fullCalendar.setFirstDay(DayOfWeek.MONDAY);
        fullCalendar.addTimeslotClickedListener(event -> {
            Date d = new Date();
            d.setStart(event.getDateTime().withHour(19));
            d.setEnd(d.getStart().plusHours(1));
            createEditDateDialog(d).open();
        });
        fullCalendar.addEntryClickedListener(event -> {
            if (event.getEntry() instanceof DateEntry dateEntry) {
                createDateOverviewDialog(dateEntry.getDate(), this.person);
            }
        });
        fullCalendar.addDatesRenderedListener(event -> {
            LocalDate intervalStart = event.getIntervalStart();
            Locale locale = fullCalendar.getLocale();
            this.selectedStartDateIndicator.setText(
                    selectedCalendarLayout.equals(Layout.LIST) ? "Nächste 20" :
                    switch (this.selectedCalendarLayout.getCalendarView()) {
                        default -> intervalStart.format(DateTimeFormatter.ofPattern("MMMM yyyy").withLocale(locale));
                        case TIME_GRID_DAY, DAY_GRID_DAY, LIST_DAY ->
                                intervalStart.format(DateTimeFormatter.ofPattern("dd.MM.yyyy").withLocale(locale));
                        case TIME_GRID_WEEK, DAY_GRID_WEEK, LIST_WEEK ->
                                intervalStart.format(DateTimeFormatter.ofPattern("dd.MM.yy").withLocale(locale)) +
                                        " - " + intervalStart.plusDays(6)
                                        .format(DateTimeFormatter.ofPattern("dd.MM.yy").withLocale(locale)) +
                                        " (kw " + intervalStart.format(DateTimeFormatter.ofPattern("ww").withLocale(locale)) + ")";
                        case LIST_YEAR -> intervalStart.format(DateTimeFormatter.ofPattern("yyyy").withLocale(locale));
                    }
            );
        });
        fullCalendar.setPrefetchEnabled(true);
        CallbackEntryProvider<Entry> entryProvider = EntryProvider.fromCallbacks(
                query ->
                        this.selectedCalendarLayout == Layout.LIST_YEAR ?
                                dateRepository.findByStartBetweenAndGroupMembersIn(LocalDateTime.now(), LocalDateTime.now().plusYears(1), this.person).sorted().limit(20).map(DateEntry::new)
                                : dateRepository.findByStartBetweenAndGroupMembersIn(query.getStart(), query.getEnd(), this.person).map(DateEntry::new)
                ,
                entryId -> dateRepository.findById(Long.valueOf(entryId)).map(DateEntry::new).orElse(null)
        );
        fullCalendar.setEntryProvider(entryProvider);
        this.add(fullCalendar);
        this.setFlexGrow(1, fullCalendar);

        previous.addThemeVariants(ButtonVariant.LUMO_LARGE);
        previous.addClickListener(event -> UI.getCurrent().navigate(CalendarView.class,
                new RouteParameters(new RouteParam("layout", this.selectedCalendarLayout.name()), new RouteParam("week", this.selectedStartDate.minus(1, this.selectedCalendarLayout.getStepSize()).toString()))));

        today.addThemeVariants(ButtonVariant.LUMO_LARGE, ButtonVariant.LUMO_PRIMARY);
        today.setTooltipText("Zu heute springen");
        today.addClickListener(event -> UI.getCurrent().navigate(CalendarView.class,
                new RouteParameters(new RouteParam("layout", this.selectedCalendarLayout.name()), new RouteParam("week", LocalDate.now().toString()))));

        next.addThemeVariants(ButtonVariant.LUMO_LARGE);
        next.addClickListener(event -> UI.getCurrent().navigate(CalendarView.class,
                new RouteParameters(new RouteParam("layout", this.selectedCalendarLayout.name()), new RouteParam("week", this.selectedStartDate.plus(1, this.selectedCalendarLayout.getStepSize()).toString()))));
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        this.selectedCalendarLayout = beforeEnterEvent.getRouteParameters().get("layout").map(Layout::valueOf).orElse(Layout.LIST_YEAR);
        this.fullCalendar.changeView(this.selectedCalendarLayout.getCalendarView());
        this.fullCalendar.getEntryProvider().refreshAll();
        this.selectedStartDate = beforeEnterEvent.getRouteParameters().get("week").map(LocalDate::parse).orElse(LocalDate.now());
        LocalDate calDate = this.selectedCalendarLayout.getStepSize() == null ? LocalDate.now() : this.selectedStartDate;
        fullCalendar.gotoDate(calDate);
        previous.setEnabled(this.selectedCalendarLayout != Layout.LIST_YEAR);
        today.setEnabled(this.selectedCalendarLayout != Layout.LIST_YEAR);
        next.setEnabled(this.selectedCalendarLayout != Layout.LIST_YEAR);
    }

    @Getter
    public static class DateEntry extends Entry {
        private final Date date;

        public DateEntry(Date date) {
            this.date = date;
            this.setColor(date.getGroup().getColor());
            this.setTitle(date.getTitle());
            this.setStart(date.getStart());
            this.setEnd(date.getEnd());
        }
    }

    @Getter
    public enum Layout {
        LIST(CalendarViewImpl.LIST_MONTH, ChronoUnit.MONTHS, "MONATSLISTE"),
        LIST_YEAR(CalendarViewImpl.LIST_YEAR, ChronoUnit.YEARS, "NÄCHSTE 20"),
        MONTH(CalendarViewImpl.DAY_GRID_MONTH, ChronoUnit.MONTHS, "MONAT"),
        YEAR(CalendarViewImpl.MULTI_MONTH, ChronoUnit.YEARS, "JAHR");

        private final CalendarViewImpl calendarView;
        private final TemporalUnit stepSize;
        private final String displayName;

        Layout(CalendarViewImpl calendarView, TemporalUnit stepSize, String displayName) {
            this.calendarView = calendarView;
            this.stepSize = stepSize;
            this.displayName = displayName;
        }
    }

    private void createDateOverviewDialog(Date date, Person person) {
        ClosableDialog dateOverview = new ClosableDialog();
        HorizontalLayout header = new HorizontalLayout();
        header.setAlignItems(FlexComponent.Alignment.CENTER);

        header.add(new H3(date.getTitle()));

        Button edit = new Button(VaadinIcon.PENCIL.create());
        edit.setVisible(date.getGroup().getAdmins().contains(person));
        edit.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        edit.addClickListener(event -> createEditDateDialog(date).open());
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
                        dateOverview.close();
                    }
            );
            confirmDelete.setCancelable(true);
            confirmDelete.setCancelText("Abbruch");
            confirmDelete.setCloseOnEsc(true);
            confirmDelete.open();
        });

        header.add(delete);
        dateOverview.setTitle(header);
        dateOverview.setCloseListener(() -> fullCalendar.getEntryProvider().refreshAll());
        dateOverview.add(createDateWidget(date, this.person));
        dateOverview.open();
    }

    private Component createDateWidget(Date date, Person person) {
        VerticalLayout content = new VerticalLayout();
        content.setPadding(true);

        Feedback.Status status = date.getStatusFor(person);

        NativeLabel time = new NativeLabel((date.getStart().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM))) +
                (date.getEnd() == null ? "" : (" - " + date.getEnd().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)))));
        content.add(time);


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
        committedAvatars.setItems(getAvatars(date, Feedback.Status.COMMITTED));
        committedWithLabel.add(committedAvatars);

        HorizontalLayout cancelledWithLabel = new HorizontalLayout();
        cancelledWithLabel.add(new NativeLabel("Absagen:"));
        cancelledWithLabel.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        cancelledWithLabel.setAlignItems(FlexComponent.Alignment.CENTER);
        cancelledWithLabel.setWidth("50%");

        AvatarGroup cancelledAvatars = new AvatarGroup();
        cancelledAvatars.setMaxItemsVisible(4);
        cancelledAvatars.addThemeVariants(AvatarGroupVariant.LUMO_SMALL);
        cancelledAvatars.setItems(getAvatars(date, Feedback.Status.CANCELLED));
        cancelledWithLabel.add(cancelledAvatars);

        feedbacks.add(committedWithLabel, cancelledWithLabel);
        feedbacks.addClickListener(event -> feedbackOverviewDialog(date, person).open());
        feedbacks.setHeight(committedAvatars.getHeight());
        content.add(feedbacks);

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
        content.add(footer);

        NativeLabel pollStoppedLabel = new NativeLabel("Die Abfrage läuft nicht mehr");
        pollStoppedLabel.setVisible(!pollRunning);
        content.add(pollStoppedLabel);

        commit.addClickListener(event -> {// TODO Direkt das Feedback zu ändern wirft eine Fehlermeldung, erst beim zweiten öffnen möglich
            Feedback feedback = Feedback.create(person, Feedback.Status.COMMITTED);
            feedbackRepository.save(feedback);
            date.addFeedback(feedback);
            dateRepository.save(date);
            committedAvatars.setItems(getAvatars(date, Feedback.Status.COMMITTED));
            cancelledAvatars.setItems(getAvatars(date, Feedback.Status.CANCELLED));
            commit.setEnabled(false);
            cancel.setEnabled(true);
        });
        cancel.addClickListener(event -> {
            Feedback feedback = Feedback.create(person, Feedback.Status.CANCELLED);
            feedbackRepository.save(feedback);
            date.addFeedback(feedback);
            dateRepository.save(date);
            committedAvatars.setItems(getAvatars(date, Feedback.Status.COMMITTED));
            cancelledAvatars.setItems(getAvatars(date, Feedback.Status.CANCELLED));
            commit.setEnabled(true);
            cancel.setEnabled(false);
        });
        return content;
    }

    private List<AvatarGroup.AvatarGroupItem> getAvatars(Date date, Feedback.Status status) {
        return date.getGroup().getMembers().stream().filter(p -> date.getStatusFor(p).equals(status)).map(Person::getAvatarGroupItem).toList();
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

    private Dialog createEditDateDialog(Date date) {
        ClosableDialog dialog = new ClosableDialog();
        dialog.setTitle(new H3("Termin erstellen"));
        dialog.setCloseListener(() -> fullCalendar.getEntryProvider().refreshAll());

        Binder<Date> binder = new Binder<>();

        FormLayout content = new FormLayout();
        content.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2)
        );

        TextField boardTitle = new TextField("Titel des Termins");
        boardTitle.setRequired(true);
        binder.forField(boardTitle).withValidator(new NonNullValidator<>()).bind(Date::getTitle, Date::setTitle);
        content.add(boardTitle);

        ComboBox<Group> selectGroup = new ComboBox<>();
        selectGroup.setLabel("Gruppe");
        selectGroup.setRequired(true);
        selectGroup.setAllowCustomValue(true);
        selectGroup.addCustomValueSetListener(event -> {
            Group group = new Group();
            group.setName(event.getDetail());
            group.getAdmins().add(person);
            group.getMembers().add(person);
            selectGroup.setValue(group);
        });
        binder.forField(selectGroup).withValidator(new NonNullValidator<>()).bind(Date::getGroup, Date::setGroup);
        selectGroup.setItems(groupRepository.findByAdminsIn(person));
        content.add(selectGroup);

        ComboBox<Organisation> selectOrganisation = new ComboBox<>();
        selectOrganisation.setLabel("Organisation");
        selectOrganisation.setVisible(false);
        selectOrganisation.setRequired(true);
        selectOrganisation.setAllowCustomValue(true);
        selectOrganisation.setItems(organisationRepository.findByMembersIn(this.person));
        if (date.getGroup() != null) {
            selectOrganisation.setValue(date.getGroup().getOrganisation());
        }
        selectOrganisation.addCustomValueSetListener(event -> {
            Organisation organisation = new Organisation();
            organisation.setName(event.getDetail());
            organisation.setAdmin(this.person);
            organisation.getMembers().add(this.person);
            selectOrganisation.setValue(organisation);
        });
        content.add(selectOrganisation);
        content.setColspan(selectOrganisation, 2);

        selectGroup.addValueChangeListener(event -> selectOrganisation.setVisible(event.getValue().getOrganisation() == null));


        DateTimePicker startPicker = new DateTimePicker("Von");// Das Datum wird hier als UTC+2 eingegeben und im DateWidget auch als solches angezeigt. Dass der Kalender aber die Zeitzone des Besuchers automatisch setzt, sind diese als UTC/0 interpretiert und werden entsprechend verschoben um +2 Stunden
        startPicker.setStep(Duration.of(30, ChronoUnit.MINUTES));
        startPicker.setRequiredIndicatorVisible(true);
        binder.forField(startPicker).withValidator(new NonNullValidator<>()).bind(Date::getStart, Date::setStart);
        content.add(startPicker);

        DateTimePicker endPicker = new DateTimePicker("Bis");
        endPicker.setStep(Duration.of(30, ChronoUnit.MINUTES));
        endPicker.setRequiredIndicatorVisible(true);
        binder.forField(endPicker)
                .withValidator((s, valueContext) ->
                        s.isAfter(startPicker.getValue()) ?
                                ValidationResult.ok() :
                                ValidationResult.error("Das Ende einer Veranstaltung kann nicht vor dessen Beginn liegen"))
                .bind(Date::getEnd, Date::setEnd);
        startPicker.addValueChangeListener(event -> {
            if (event.getValue() != null && (endPicker.getValue() == null || endPicker.getValue().isBefore(event.getValue()))) {
                endPicker.setValue(event.getValue().plusHours(1));
            }
        });
        content.add(endPicker);

        ComboBox<Integer> selectRepetitionInterval = new ComboBox<>();
        selectRepetitionInterval.setAllowCustomValue(true);
        selectRepetitionInterval.setLabel("Terminwiederholung");
        selectRepetitionInterval.setItems(0, 1, 2, 7, 14, 21);
        selectRepetitionInterval.setValue(0);
        selectRepetitionInterval.setItemLabelGenerator(value -> {
            if (value == 0) {
                return "Keine Wiederholung";
            } else if (value == 1) {
                return "jeden Tag";
            } else if (value == 7) {
                return "jede Woche";
            } else if (value == 14) {
                return "alle zwei Wochen";
            } else {
                return "alle " + value + " Tage";
            }
        });
        content.add(selectRepetitionInterval);

        DatePicker endOfSeries = new DatePicker("Ende der Terminserie");
        endOfSeries.setValue(LocalDate.now().plusYears(1));
        endOfSeries.setEnabled(false);
        content.add(endOfSeries);

        selectRepetitionInterval.addValueChangeListener(event -> endOfSeries.setEnabled(event.getValue() != 0));

        Checkbox publish = new Checkbox("Veröffentlichen");
        publish.setVisible(false);// TODO
        binder.forField(publish).bind(Date::isPublish, Date::setPublish);
        content.add(publish);

        dialog.add(content);

        binder.readBean(date);

        HorizontalLayout footer = new HorizontalLayout();
        footer.setWidthFull();
        footer.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        Button save = new Button("Speichern", VaadinIcon.SAFE.create());
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        save.addClickShortcut(Key.ENTER);
        save.addClickListener(event -> {
            try {
                binder.writeBean(date);
                if (selectOrganisation.isVisible()) {
                    if (selectOrganisation.getValue() != null) {
                        if (date.getGroup().getOrganisation() == null) {
                            organisationRepository.save(selectOrganisation.getValue());
                            date.getGroup().setOrganisation(selectOrganisation.getValue());
                        }
                    } else {
                        selectOrganisation.setInvalid(true);
                        selectOrganisation.setErrorMessage("Bitte wähle eine Organisation aus, zu der die neue Gruppe gehören soll");
                        return;
                    }
                }
                groupRepository.save(date.getGroup());
                dateRepository.save(date);
                if (selectRepetitionInterval.getValue() != 0) {
                    Date dateCopy = new Date(date);
                    LocalDate localDate = date.getStart().toLocalDate().plusDays(selectRepetitionInterval.getValue());
                    while (localDate.isBefore(endOfSeries.getValue())) {
                        dateCopy.setStart(dateCopy.getStart().plusDays(selectRepetitionInterval.getValue()));
                        dateCopy.setEnd(dateCopy.getEnd().plusDays(selectRepetitionInterval.getValue()));
                        dateCopy = new Date(dateCopy);
                        dateRepository.save(dateCopy);
                        localDate = localDate.plusDays(selectRepetitionInterval.getValue());
                    }
                }
                dialog.close();
            } catch (ValidationException ignored) {
            }
        });
        footer.add(save);
        dialog.getFooter().add(footer);
        return dialog;
    }
}