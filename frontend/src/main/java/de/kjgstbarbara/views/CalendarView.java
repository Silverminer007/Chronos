package de.kjgstbarbara.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.*;
import com.vaadin.flow.router.*;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.theme.lumo.LumoIcon;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.kjgstbarbara.components.*;
import de.kjgstbarbara.components.Header;
import de.kjgstbarbara.data.*;
import de.kjgstbarbara.service.*;
import de.kjgstbarbara.views.date.DateView;
import de.kjgstbarbara.views.security.RegisterView;
import jakarta.annotation.security.PermitAll;
import lombok.Getter;
import org.springframework.security.core.userdetails.User;
import org.vaadin.stefan.fullcalendar.*;
import org.vaadin.stefan.fullcalendar.dataprovider.CallbackEntryProvider;
import org.vaadin.stefan.fullcalendar.dataprovider.EntryProvider;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Route(value = "calendar/:page?", layout = MainNavigationView.class)
@RouteAlias(value = ":page?", layout = MainNavigationView.class)
@PageTitle("Meine Termine")
@PermitAll
public class CalendarView extends VerticalLayout implements BeforeEnterObserver {

    private final PersonsRepository personsRepository;
    private final OrganisationRepository organisationRepository;
    private final DateRepository dateRepository;
    private final GroupRepository groupRepository;
    private final FeedbackRepository feedbackRepository;

    private final Person person;

    private final FullCalendar fullCalendar;
    private final VerticalLayout dateListLayout = new VerticalLayout();
    private final Scroller dateListScroller;

    private final H4 shownIntervalLabel = new H4();
    private int page = 0;

    private String search = null;

    public CalendarView(PersonsService personsService, OrganisationService organisationService, DatesService datesService, GroupService groupService, FeedbackService feedbackService, AuthenticationContext authenticationContext) {
        this.personsRepository = personsService.getPersonsRepository();
        this.organisationRepository = organisationService.getOrganisationRepository();
        this.dateRepository = datesService.getDateRepository();
        this.groupRepository = groupService.getGroupRepository();
        this.feedbackRepository = feedbackService.getFeedbackRepository();
        this.person = authenticationContext.getAuthenticatedUser(User.class)
                .flatMap(userDetails -> personsRepository.findByUsername(userDetails.getUsername()))
                .orElse(null);
        this.fullCalendar = setupCalendar();
        this.dateListScroller = new Scroller(this.setupDateList());
        if (person == null) {
            UI.getCurrent().navigate(RegisterView.class);
        } else {
            this.setPadding(false);
            this.setSpacing(false);
            this.setSizeFull();

            this.add(createHeaderBar());
            this.add(this.fullCalendar);
            this.dateListScroller.setSizeFull();
            this.add(this.dateListScroller);
            this.selectCalendarLayout();
            this.add(createFooter());
        }
    }

    private void selectCalendarLayout() {
        if (this.search == null &&
                (this.person.getCalendarLayout() == Person.CalendarLayout.MONTH || this.person.getCalendarLayout() == Person.CalendarLayout.YEAR)) {
            this.fullCalendar.setVisible(true);
            this.dateListScroller.setVisible(false);
        } else {
            this.fullCalendar.setVisible(false);
            this.dateListScroller.setVisible(true);
        }
    }

    private HorizontalLayout createHeaderBar() {
        HorizontalLayout header = new Header();

        header.add(this.shownIntervalLabel);

        header.add(new Search(searchString -> {
            this.search = searchString;
            this.selectCalendarLayout();
            this.setupDateList();
        }));
        return header;
    }

    private FullCalendar setupCalendar() {
        FullCalendar fullCalendar = FullCalendarBuilder.create().build();
        if (this.person == null) {// Wenn this.person null ist, wird der View eh direkt verlassen und so lassen sich NullPointerException Warnungen vermeiden
            return fullCalendar;
        }
        fullCalendar.setTimezone(new Timezone(this.person.getTimezone()));
        fullCalendar.setLocale(this.person.getUserLocale());
        fullCalendar.setSizeFull();
        fullCalendar.addThemeVariants(FullCalendarVariant.LUMO);
        fullCalendar.setFirstDay(DayOfWeek.MONDAY);
        fullCalendar.addTimeslotClickedListener(event -> {
            Date d = new Date();
            d.setStart(event.getDateTime().withHour(19));
            d.setEnd(d.getStartAtTimezone(ZoneOffset.UTC).plusHours(1));
            createCreateDateDialog(d).open();
        });
        fullCalendar.addEntryClickedListener(event -> {
            if (event.getEntry() instanceof DateEntry dateEntry) {
                UI.getCurrent().navigate(DateView.class, new RouteParameters(new RouteParam("date", dateEntry.getDate().getId())));
            }
        });
        fullCalendar.setPrefetchEnabled(true);
        fullCalendar.changeView(switch (this.person.getCalendarLayout()) {
            case LIST_PER_MONTH:
                yield CalendarViewImpl.LIST_MONTH;
            case LIST_NEXT:
                yield CalendarViewImpl.LIST_YEAR;
            case MONTH:
                yield CalendarViewImpl.DAY_GRID_MONTH;
            case YEAR:
                yield CalendarViewImpl.MULTI_MONTH;
        });
        CallbackEntryProvider<Entry> entryProvider = EntryProvider.fromCallbacks(
                query -> dateRepository.findByStartBetweenAndGroupMembersInAndGroupOrganisationMembersIn(query.getStart(), query.getEnd(), this.person).map(DateEntry::new)
                ,
                entryId -> dateRepository.findById(Long.valueOf(entryId)).map(DateEntry::new).orElse(null)
        );
        fullCalendar.setEntryProvider(entryProvider);
        this.add(fullCalendar);
        this.setFlexGrow(1, fullCalendar);
        return fullCalendar;
    }

    private Component setupDateList() {
        if (this.person == null) {
            return dateListLayout;
        }
        List<Date> dates = findSubListOfDates();

        dateListLayout.removeAll();
        dateListLayout.setSizeFull();
        LocalDate lastDate = null;
        for (Date date : dates) {
            if (lastDate == null || !date.getStartAtTimezone(this.person.getTimezone()).toLocalDate().isEqual(lastDate)) {
                lastDate = date.getStartAtTimezone(this.person.getTimezone()).toLocalDate();
                H4 dateLabel = new H4(lastDate.format(DateTimeFormatter.ofPattern("EEE, dd.MM.yyyy")));
                dateListLayout.add(dateLabel);
            }
            HorizontalLayout dateEntry = new HorizontalLayout();
            dateEntry.setWidthFull();
            dateEntry.setAlignItems(Alignment.CENTER);
            dateEntry.setJustifyContentMode(JustifyContentMode.END);
            dateEntry.addClassNames(LumoUtility.Background.TINT_5, LumoUtility.BorderRadius.SMALL);

            HorizontalLayout dateInformation = new HorizontalLayout();
            dateInformation.setWidthFull();
            dateInformation.setJustifyContentMode(JustifyContentMode.START);
            dateInformation.setAlignItems(Alignment.CENTER);
            dateInformation.addClickListener(event -> UI.getCurrent().navigate(DateView.class, new RouteParameters(new RouteParam("date", date.getId()))));

            dateInformation.add(new NativeLabel());

            Icon circleIcon = VaadinIcon.CIRCLE.create();
            circleIcon.setColor(date.getGroup().getColor());
            circleIcon.setSize("10px");
            dateInformation.add(circleIcon);

            NativeLabel time = new NativeLabel(date.getStartAtTimezone(this.person.getTimezone()).format(DateTimeFormatter.ofPattern("HH:mm")) + " Uhr");
            dateInformation.add(time);

            NativeLabel dateTitle = new NativeLabel(date.getTitle());
            dateInformation.add(dateTitle);
            dateEntry.add(dateInformation);

            HorizontalLayout feedbackButtons = new HorizontalLayout();
            feedbackButtons.setJustifyContentMode(JustifyContentMode.END);
            feedbackButtons.setAlignItems(Alignment.CENTER);

            Feedback.Status currentStatus = date.getStatusFor(this.person);
            FeedbackButton commit = new FeedbackButton(Feedback.Status.COMMITTED, false, !Feedback.Status.COMMITTED.equals(currentStatus));
            FeedbackButton cancel = new FeedbackButton(Feedback.Status.CANCELLED, false, !Feedback.Status.CANCELLED.equals(currentStatus));
            commit.addClickListener(event -> {
                Date d = dateRepository.findById(date.getId()).orElse(null);
                if (d == null) {
                    return;
                }
                Feedback feedback = new Feedback(person, Feedback.Status.COMMITTED);
                d.addFeedback(feedback);
                feedbackRepository.save(feedback);
                dateRepository.save(d);
                commit.setEnabled(false);
                cancel.setEnabled(true);
            });
            cancel.addClickListener(event -> {
                Date d = dateRepository.findById(date.getId()).orElse(null);
                if (d == null) {
                    return;
                }
                Feedback feedback = new Feedback(person, Feedback.Status.CANCELLED);
                d.addFeedback(feedback);
                feedbackRepository.save(feedback);
                dateRepository.save(d);
                commit.setEnabled(true);
                cancel.setEnabled(false);
            });
            if (date.isPollRunning() && date.getStartAtTimezone(ZoneOffset.UTC).isAfter(LocalDateTime.now(ZoneOffset.UTC))) {
                feedbackButtons.add(commit, cancel);
            } else {
                if (!commit.isEnabled()) {
                    feedbackButtons.add(commit);
                } else if (!cancel.isEnabled()) {
                    feedbackButtons.add(cancel);
                } else {
                    feedbackButtons.add(new FeedbackButton(Feedback.Status.NONE, false, false));
                }
            }

            dateEntry.add(feedbackButtons);

            dateListLayout.add(dateEntry);
        }
        if (dates.isEmpty()) {
            if (this.search != null) {
                dateListLayout.add(new H6("Keine Termine gefunden"));
            } else {
                dateListLayout.add(new H6("Bisher keine Termine zu sehen"));
            }
        }
        return dateListLayout;
    }

    private List<Date> findSubListOfDates() {
        if (Person.CalendarLayout.LIST_PER_MONTH.equals(this.person.getCalendarLayout())) {
            return dateRepository.findByStartBetweenAndGroupMembersInAndGroupOrganisationMembersIn(LocalDateTime.now(ZoneOffset.UTC).plusMonths(this.page).withDayOfMonth(1).withHour(0).withMinute(0), LocalDateTime.now(ZoneOffset.UTC).plusMonths(1 + this.page).withDayOfMonth(1).minusDays(1).withHour(0).withMinute(0), this.person).sorted().toList();
        }
        return dateRepository.calendarQuery(this.search, this.page, this.person);
    }

    private HorizontalLayout createFooter() {
        HorizontalLayout footer = new HorizontalLayout();
        footer.setSpacing(true);
        footer.setPadding(true);
        footer.addClassNames(LumoUtility.Width.FULL,
                LumoUtility.JustifyContent.BETWEEN,
                LumoUtility.AlignSelf.STRETCH);
        footer.setAlignItems(Alignment.CENTER);

        Button previousButton = new Button(LumoIcon.ARROW_LEFT.create());
        previousButton.addThemeVariants(ButtonVariant.LUMO_LARGE, ButtonVariant.LUMO_CONTRAST);
        previousButton.addClickListener(event -> this.previous());
        previousButton.addClickShortcut(Key.ARROW_LEFT);
        footer.add(previousButton);

        Button createButton = new Button("Erstellen", VaadinIcon.PLUS.create());
        createButton.setTooltipText("Einen neuen Termin erstellen");
        createButton.setAriaLabel("Neuer Termin");
        createButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST, ButtonVariant.LUMO_LARGE);
        createButton.addClickListener(event -> createCreateDateDialog(new Date()).open());
        footer.add(createButton);

        Button nextButton = new Button(LumoIcon.ARROW_RIGHT.create());
        nextButton.addThemeVariants(ButtonVariant.LUMO_LARGE, ButtonVariant.LUMO_CONTRAST);
        nextButton.addClickShortcut(Key.ARROW_RIGHT);
        nextButton.addClickListener(event -> this.next());
        footer.add(nextButton);
        return footer;
    }

    private void next() {
        UI.getCurrent().navigate(CalendarView.class,
                new RouteParameters(new RouteParam("page", this.page + 1)));
    }

    private void previous() {
        UI.getCurrent().navigate(CalendarView.class,
                new RouteParameters(new RouteParam("page", this.page - 1)));
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        if (person == null) {
            beforeEnterEvent.rerouteTo(RegisterView.class);
            return;
        }
        this.page = beforeEnterEvent.getRouteParameters().get("page").map(Integer::parseInt).orElse(0);
        this.fullCalendar.getEntryProvider().refreshAll();
        LocalDate calDate = switch (this.person.getCalendarLayout()) {
            case LIST_PER_MONTH, MONTH -> LocalDate.now(ZoneOffset.UTC).plusMonths(this.page);
            case LIST_NEXT -> LocalDate.now(ZoneOffset.UTC);
            case YEAR -> LocalDate.now(ZoneOffset.UTC).plusYears(this.page);
        };
        fullCalendar.gotoDate(calDate);
        this.setupDateList();
        this.shownIntervalLabel.setText(
                switch (this.person.getCalendarLayout()) {
                    case LIST_NEXT -> {
                        if (this.page < 0) {
                            int intervalStart = ((this.page * -1) - 1) * 20 + 1;
                            yield "Vergangenheit " + intervalStart + " - " + (intervalStart + 19);
                        } else {
                            int intervalStart = this.page * 20 + 1;
                            yield "Zukunft " + intervalStart + " - " + (intervalStart + 19);
                        }
                    }
                    case YEAR ->
                            LocalDate.now(ZoneOffset.UTC).plusYears(this.page).format(DateTimeFormatter.ofPattern("yyyy").withLocale(this.person.getUserLocale()));
                    default ->
                            LocalDate.now(ZoneOffset.UTC).plusMonths(this.page).format(DateTimeFormatter.ofPattern("MMMM yyyy").withLocale(this.person.getUserLocale()));
                }
        );
    }

    @Getter
    public static class DateEntry extends Entry {
        private final Date date;

        public DateEntry(Date date) {
            this.date = date;
            this.setColor(date.getGroup().getColor());
            this.setTitle(date.getTitle());
            this.setStart(date.getStartAtTimezone(ZoneOffset.UTC));
            this.setEnd(date.getEndAtTimezone(ZoneOffset.UTC));
        }
    }

    private Dialog createCreateDateDialog(Date date) {
        ClosableDialog dialog = new ClosableDialog();
        dialog.setTitle(new H3("Termin erstellen"));
        dialog.setCloseListener(() -> fullCalendar.getEntryProvider().refreshAll());
        dialog.setMaxWidth("800px");

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

        DateTimePicker startPicker = new DateTimePicker("Von");
        startPicker.setStep(Duration.of(30, ChronoUnit.MINUTES));
        startPicker.setRequiredIndicatorVisible(true);
        binder.forField(startPicker).withValidator(new NonNullValidator<>()).withConverter(new TimeZoneConverter(person.getTimezone())).bind(Date::getStart, Date::setStart);
        content.add(startPicker);

        DateTimePicker endPicker = new DateTimePicker("Bis");
        endPicker.setStep(Duration.of(30, ChronoUnit.MINUTES));
        endPicker.setRequiredIndicatorVisible(true);
        binder.forField(endPicker)
                .withValidator((s, valueContext) ->
                        s.isAfter(startPicker.getValue()) ?
                                ValidationResult.ok() :
                                ValidationResult.error("Das Ende einer Veranstaltung kann nicht vor dessen Beginn liegen"))
                .withConverter(new TimeZoneConverter(person.getTimezone()))
                .bind(Date::getEnd, Date::setEnd);
        startPicker.addValueChangeListener(event -> {
            if (event.getValue() != null && (endPicker.getValue() == null || endPicker.getValue().isBefore(event.getValue()))) {
                endPicker.setValue(event.getValue().plusHours(1));
            }
        });
        content.add(endPicker);

        TextField venue = new TextField("Veranstaltungsort");
        binder.forField(venue).bind(Date::getVenue, Date::setVenue);
        content.add(venue);

        TextArea notes = new TextArea("Notizen");
        binder.forField(notes).bind(Date::getNotes, Date::setNotes);
        content.add(notes);
        content.setColspan(notes, 2);

        ComboBox<Integer> selectRepetitionInterval = createRepetitionIntervalSelector();
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

        dialog.getFooter().add(new DialogFooter(dialog::close, () -> {
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
                Date d = dateRepository.save(date);
                if (selectRepetitionInterval.getValue() != 0) {
                    d.setRepetitionRule(selectRepetitionInterval.getValue());
                    d.setLinkedTo(d.getId());
                    d.setEndOfRepetition(endOfSeries.getValue());
                }
                dateRepository.save(d);
                int countDates = 1;
                if (selectRepetitionInterval.getValue() != 0) {
                    Date dateCopy = new Date(d);
                    LocalDate localDate = d.getStartAtTimezone(ZoneOffset.UTC).toLocalDate().plusDays(selectRepetitionInterval.getValue());
                    while (localDate.isBefore(endOfSeries.getValue())) {
                        countDates++;
                        dateCopy.setStart(dateCopy.getStartAtTimezone(ZoneOffset.UTC).plusDays(selectRepetitionInterval.getValue()));
                        dateCopy.setEnd(dateCopy.getEndAtTimezone(ZoneOffset.UTC).plusDays(selectRepetitionInterval.getValue()));
                        dateCopy = new Date(dateCopy);
                        dateRepository.save(dateCopy);
                        localDate = localDate.plusDays(selectRepetitionInterval.getValue());
                    }
                }
                UI.getCurrent().navigate(CalendarView.class, new RouteParameters(new RouteParam("page", 0)));
                if (selectRepetitionInterval.getValue() != null) {
                    Notification.show("Es wurden " + countDates + " Termine erstellt");
                } else {
                    Notification.show("\"" + date.getTitle() + "\" wurde erstellt");
                }
                dialog.close();
            } catch (ValidationException ignored) {
            }
        }, "Erstellen"));
        return dialog;
    }

    private static ComboBox<Integer> createRepetitionIntervalSelector() {
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
        return selectRepetitionInterval;
    }
}