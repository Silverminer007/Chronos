package de.kjgstbarbara.views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.theme.lumo.LumoIcon;
import de.kjgstbarbara.data.Date;
import de.kjgstbarbara.data.Person;
import de.kjgstbarbara.messaging.SenderUtils;
import de.kjgstbarbara.service.*;
import de.kjgstbarbara.views.components.DateWidget;
import de.kjgstbarbara.views.components.EditDateDialog;
import de.kjgstbarbara.views.nav.MainNavigationView;
import jakarta.annotation.security.PermitAll;
import lombok.Getter;
import org.springframework.security.core.userdetails.UserDetails;
import org.vaadin.stefan.fullcalendar.*;
import org.vaadin.stefan.fullcalendar.dataprovider.CallbackEntryProvider;
import org.vaadin.stefan.fullcalendar.dataprovider.EntryProvider;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Locale;

@Route(value = "calendar/:week?/:layout?", layout = MainNavigationView.class)
@RouteAlias(value = ":week?/:layout?", layout = MainNavigationView.class)
@PageTitle("Meine Termine")
@PermitAll
public class CalendarView extends VerticalLayout implements BeforeEnterObserver {
    private final PersonsRepository personsRepository;
    private final DateRepository dateRepository;
    private final GroupRepository groupRepository;
    private final FeedbackRepository feedbackRepository;

    private final Person person;

    private final FullCalendar fullCalendar = FullCalendarBuilder.create().withAutoBrowserLocale()/*.withAutoBrowserTimezone()*/.build();// Siehe EditDateDialog
    private final Button cL = new Button("", VaadinIcon.ANGLE_DOWN.create());
    private final H4 intervalLabel = new H4();
    private LocalDate week = LocalDate.now();
    private Layout calendarView = Layout.LIST;

    public CalendarView(PersonsService personsService, DatesService datesService, GroupService groupService, FeedbackService feedbackService, SenderUtils senderUtils, AuthenticationContext authenticationContext) {
        this.personsRepository = personsService.getPersonsRepository();
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
        header.setAlignItems(Alignment.CENTER);
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);

        MenuBar menu = new MenuBar();
        menu.addThemeVariants(MenuBarVariant.LUMO_TERTIARY_INLINE);

        cL.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        MenuItem calendarLayout = menu.addItem(cL);
        SubMenu subMenu = calendarLayout.getSubMenu();
        for (Layout layout : Layout.values()) {
            Button changeLayout = new Button(layout.getDisplayName());
            changeLayout.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
            changeLayout.addClickListener(event -> UI.getCurrent().navigate(CalendarView.class,
                    new RouteParameters(new RouteParam("layout", layout.name()), new RouteParam("week", this.week.toString()))));
            subMenu.addItem(changeLayout);
        }
        header.add(menu);

        header.add(intervalLabel);

        Button createNew = new Button("Neuer Termin", VaadinIcon.PLUS_SQUARE_O.create());
        createNew.addClickListener(event -> {
            Date d = new Date();
            new EditDateDialog(d, person, groupRepository, dateRepository).setCloseListener(() -> fullCalendar.getEntryProvider().refreshAll()).open();
        });
        header.add(createNew);

        this.add(header);

        fullCalendar.setSizeFull();
        fullCalendar.addThemeVariants(FullCalendarVariant.LUMO);
        fullCalendar.setFirstDay(DayOfWeek.MONDAY);
        fullCalendar.addTimeslotClickedListener(event -> {
            Date d = new Date();
            d.setStart(event.getDateTime().withHour(19));
            d.setEnd(d.getStart().plusHours(1));
            new EditDateDialog(d, person, groupRepository, dateRepository).setCloseListener(() -> fullCalendar.getEntryProvider().refreshAll()).open();
        });
        fullCalendar.addEntryClickedListener(event -> {
            if (event.getEntry() instanceof DateEntry dateEntry) {
                new DateWidget(dateEntry.getDate(), feedbackRepository, dateRepository, groupRepository, senderUtils, this.person).setCloseListener(() -> fullCalendar.getEntryProvider().refreshAll()).open();
            }
        });
        fullCalendar.addDatesRenderedListener(event -> {
            LocalDate intervalStart = event.getIntervalStart();
            Locale locale = fullCalendar.getLocale();
            this.intervalLabel.setText(
                    switch (this.calendarView.getCalendarView()) {
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
                query -> dateRepository.findByStartBetweenAndGroupMembersIn(query.getStart(), query.getEnd(), this.person).map(DateEntry::new),
                entryId -> dateRepository.findById(Long.valueOf(entryId)).map(DateEntry::new).orElse(null)
        );
        fullCalendar.setEntryProvider(entryProvider);
        this.add(fullCalendar);
        this.setFlexGrow(1, fullCalendar);

        this.add(new Hr());

        HorizontalLayout buttons = new HorizontalLayout();
        buttons.setWidthFull();
        buttons.setJustifyContentMode(JustifyContentMode.CENTER);

        Button previous = new Button(LumoIcon.ARROW_LEFT.create());
        previous.addThemeVariants(ButtonVariant.LUMO_LARGE);
        previous.addClickListener(event -> UI.getCurrent().navigate(CalendarView.class,
                new RouteParameters(new RouteParam("layout", this.calendarView.name()), new RouteParam("week", this.week.minus(1, this.calendarView.getStepSize()).toString()))));
        buttons.add(previous);

        Button today = new Button("Heute");
        today.addThemeVariants(ButtonVariant.LUMO_LARGE);
        today.addClickListener(event -> UI.getCurrent().navigate(CalendarView.class,
                new RouteParameters(new RouteParam("layout", this.calendarView.name()), new RouteParam("week", LocalDate.now().toString()))));
        buttons.add(today);

        Button next = new Button(LumoIcon.ARROW_RIGHT.create());
        next.addThemeVariants(ButtonVariant.LUMO_LARGE);
        next.addClickListener(event -> UI.getCurrent().navigate(CalendarView.class,
                new RouteParameters(new RouteParam("layout", this.calendarView.name()), new RouteParam("week", this.week.plus(1, this.calendarView.getStepSize()).toString()))));
        buttons.add(next);

        this.add(buttons);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        this.calendarView = beforeEnterEvent.getRouteParameters().get("layout").map(Layout::valueOf).orElse(Layout.LIST);
        this.fullCalendar.changeView(this.calendarView.getCalendarView());
        cL.setText(this.calendarView.getDisplayName());
        this.week = beforeEnterEvent.getRouteParameters().get("week").map(LocalDate::parse).orElse(LocalDate.now());
        LocalDate calDate = this.calendarView.getStepSize() == null ? LocalDate.now() : this.week;
        fullCalendar.gotoDate(calDate);
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
        LIST(CalendarViewImpl.LIST_MONTH, ChronoUnit.MONTHS, "LISTE"),
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
}