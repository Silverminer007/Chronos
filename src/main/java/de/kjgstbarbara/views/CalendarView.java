package de.kjgstbarbara.views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.theme.lumo.LumoIcon;
import de.kjgstbarbara.data.Date;
import de.kjgstbarbara.data.Feedback;
import de.kjgstbarbara.data.Person;
import de.kjgstbarbara.service.*;
import de.kjgstbarbara.views.components.DateWidget;
import de.kjgstbarbara.views.components.EditDateDialog;
import de.kjgstbarbara.views.nav.MainNavigationView;
import jakarta.annotation.security.PermitAll;
import lombok.Getter;
import org.springframework.security.core.userdetails.UserDetails;
import org.vaadin.stefan.fullcalendar.CalendarViewImpl;
import org.vaadin.stefan.fullcalendar.Entry;
import org.vaadin.stefan.fullcalendar.FullCalendar;
import org.vaadin.stefan.fullcalendar.FullCalendarBuilder;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.List;

@Route(value = "calendar/:week?/:layout?", layout = MainNavigationView.class)
@RouteAlias(value = ":week?/:layout?", layout = MainNavigationView.class)
@PageTitle("Meine Termine")
@PermitAll
public class CalendarView extends VerticalLayout implements BeforeEnterObserver {
    private final PersonsRepository personsRepository;
    private final DateRepository dateRepository;
    private final BoardsRepository boardsRepository;
    private final FeedbackRepository feedbackRepository;

    private final Person person;

    private final FullCalendar fullCalendar = FullCalendarBuilder.create().withAutoBrowserLocale().withAutoBrowserTimezone().build();
    private final Button previous = new Button(LumoIcon.ARROW_LEFT.create());
    private final Button today = new Button("Heute");
    private final Button next = new Button(LumoIcon.ARROW_RIGHT.create());
    private final Button cL = new Button("", VaadinIcon.ANGLE_DOWN.create());
    private final H4 year = new H4();
    private LocalDate week = LocalDate.now();
    private Layout calendarView = Layout.LIST;

    public CalendarView(PersonsService personsService, DatesService datesService, BoardsService boardsService, FeedbackService feedbackService, AuthenticationContext authenticationContext) {
        this.personsRepository = personsService.getPersonsRepository();
        this.dateRepository = datesService.getDateRepository();
        this.boardsRepository = boardsService.getBoardsRepository();
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

        header.add(year);

        Button createNew = new Button("Neuer Termin", VaadinIcon.PLUS_SQUARE_O.create());
        createNew.addClickListener(event ->
                new EditDateDialog(new Date(), person, boardsRepository, dateRepository).open());
        header.add(createNew);

        this.add(header);

        HorizontalLayout legend = new HorizontalLayout();
        legend.setWidthFull();
        legend.setAlignItems(Alignment.CENTER);
        legend.setJustifyContentMode(JustifyContentMode.START);

        Icon inPast = VaadinIcon.CIRCLE.create();
        inPast.setColor("#615c5c");
        Icon current = VaadinIcon.CIRCLE.create();
        current.setColor("#00b6be");
        Icon confirmed = VaadinIcon.CIRCLE.create();
        confirmed.setColor("#00ff00");
        Icon declined = VaadinIcon.CIRCLE.create();
        declined.setColor("#ff0000");
        Icon feedbackNeeded = VaadinIcon.CIRCLE.create();
        feedbackNeeded.setColor("#ffff00");

        Button inPastLabel = new Button("Vergangenheit", inPast);
        inPastLabel.setEnabled(false);
        Button currentLabel = new Button("Heute", current);
        currentLabel.setEnabled(false);
        Button confirmedLabel = new Button("Zugesagt", confirmed);
        confirmedLabel.setEnabled(false);
        Button declinedLabel = new Button("Abgesagt", declined);
        declinedLabel.setEnabled(false);
        Button feedbackNeededLabel = new Button("Bitte Rückmeldung geben", feedbackNeeded);
        feedbackNeededLabel.setEnabled(false);

        legend.add(inPastLabel, currentLabel, confirmedLabel, declinedLabel, feedbackNeededLabel);
        this.add(legend);


        fullCalendar.setSizeFull();
        fullCalendar.setFirstDay(DayOfWeek.MONDAY);
        fullCalendar.addEntryClickedListener(event -> {
            if (event.getEntry() instanceof DateEntry dateEntry) {
                new DateWidget(dateEntry.getDate(), feedbackRepository, dateRepository, boardsRepository, this.person).open();
            }
        });
        for (Date d : getDates()) {
            fullCalendar.getEntryProvider().asInMemory().addEntries(new DateEntry(d, feedbackRepository.findById(Feedback.Key.create(this.person, d)).map(Feedback::getStatus).map(status -> {
                if(d.getEnd().isBefore(LocalDateTime.now())){
                    return "#615c5c";
                } else if(LocalDateTime.now().isAfter(d.getStart()) && LocalDateTime.now().isBefore(d.getEnd())) {
                    return "#00b6be";
                }else if (status.equals(Feedback.Status.IN)) {
                    return "#00ff00";
                } else if (status.equals(Feedback.Status.OUT)) {
                    return "#ff0000";
                } else {
                    return "#ffff00";
                }
            }).orElse("#ffff00")));
        }
        this.add(fullCalendar);
        this.setFlexGrow(1, fullCalendar);

        this.add(new Hr());

        HorizontalLayout buttons = new HorizontalLayout();
        buttons.setWidthFull();
        buttons.setJustifyContentMode(JustifyContentMode.CENTER);

        previous.addThemeVariants(ButtonVariant.LUMO_LARGE);
        previous.addClickListener(event -> UI.getCurrent().navigate(CalendarView.class,
                new RouteParameters(new RouteParam("layout", this.calendarView.name()), new RouteParam("week", this.week.minus(1, this.calendarView.getStepSize()).toString()))));
        buttons.add(previous);

        today.addThemeVariants(ButtonVariant.LUMO_LARGE);
        today.addClickListener(event -> UI.getCurrent().navigate(CalendarView.class,
                new RouteParameters(new RouteParam("layout", this.calendarView.name()), new RouteParam("week", LocalDate.now().toString()))));
        buttons.add(today);

        next.addThemeVariants(ButtonVariant.LUMO_LARGE);
        next.addClickListener(event -> UI.getCurrent().navigate(CalendarView.class,
                new RouteParameters(new RouteParam("layout", this.calendarView.name()), new RouteParam("week", this.week.plus(1, this.calendarView.getStepSize()).toString()))));
        buttons.add(next);

        this.add(buttons);
    }

    private List<Date> getDates() {
        List<Date> dates = new ArrayList<>();
        boardsRepository.findByPerson(this.person).stream().map(dateRepository::findByBoard).forEach(dates::addAll);
        return dates.stream().distinct().sorted().toList();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        this.calendarView = beforeEnterEvent.getRouteParameters().get("layout").map(Layout::valueOf).orElse(Layout.LIST);
        this.fullCalendar.changeView(this.calendarView.getCalendarView());
        previous.setVisible(this.calendarView.getStepSize() != null);
        today.setVisible(this.calendarView.getStepSize() != null);
        next.setVisible(this.calendarView.getStepSize() != null);
        cL.setText(this.calendarView.getDisplayName());
        this.week = beforeEnterEvent.getRouteParameters().get("week").map(LocalDate::parse).orElse(LocalDate.now());
        LocalDate calDate = this.calendarView.getStepSize() == null ? LocalDate.now() : this.week;
        fullCalendar.gotoDate(calDate);
        this.year.setText(String.valueOf(calDate.getYear()));
    }

    @Getter
    public static class DateEntry extends Entry {
        private final Date date;

        public DateEntry(Date date, String color) {
            this.date = date;
            this.setColor(color);
            this.setTitle(date.getTitle());
            this.setStart(date.getStart());
            this.setEnd(date.getEnd());
        }
    }

    @Getter
    public enum Layout {
        LIST(CalendarViewImpl.LIST_YEAR, ChronoUnit.YEARS, "LISTE"),
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