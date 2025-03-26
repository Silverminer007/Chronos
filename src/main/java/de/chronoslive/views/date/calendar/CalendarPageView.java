package de.chronoslive.views.date.calendar;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.RouteParam;
import com.vaadin.flow.router.RouteParameters;
import de.chronoslive.entitys.Date;
import de.chronoslive.entitys.Person;
import de.chronoslive.components.CreateDateDialog;
import de.chronoslive.repositorys.*;
import de.chronoslive.views.date.DateView;
import lombok.Getter;
import org.vaadin.stefan.fullcalendar.*;
import org.vaadin.stefan.fullcalendar.dataprovider.CallbackEntryProvider;
import org.vaadin.stefan.fullcalendar.dataprovider.EntryProvider;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

public abstract class CalendarPageView implements Calendar {
    private final DateRepository dateRepository;
    private final GroupRepository groupRepository;
    private final OrganisationRepository organisationRepository;

    private final Person person;

    public CalendarPageView(DateRepository dateRepository, GroupRepository groupRepository, OrganisationRepository organisationRepository, Person principal) {
        this.dateRepository = dateRepository;
        this.groupRepository = groupRepository;
        this.organisationRepository = organisationRepository;
        this.person = principal;
    }

    private FullCalendar setupCalendar() {
        FullCalendar fullCalendar = FullCalendarBuilder.create().build();
        fullCalendar.setTimezone(new Timezone(this.person.getTimezone()));
        fullCalendar.setLocale(this.person.getUserLocale());
        fullCalendar.setSizeFull();
        fullCalendar.addThemeVariants(FullCalendarVariant.LUMO);
        fullCalendar.setFirstDay(DayOfWeek.MONDAY);
        fullCalendar.addTimeslotClickedListener(event ->
                new CreateDateDialog()
                        .setCloseListener(d -> {
                            fullCalendar.getEntryProvider().refreshAll();
                            fullCalendar.gotoDate(d.getStart().toLocalDate());
                        })
                        .setPerson(person)
                        .setOrganisations(this.organisationRepository.findByMembersIn(this.person))
                        .setGroups(this.groupRepository.findByAdminsIn(this.person))
                        .setGroupSaver(this.groupRepository::save)
                        .setDateSaver(this.dateRepository::save)
                        .setOrganisationSaver(this.organisationRepository::save)
                        .setDefaultDate(event.getDate())
                        .create());
        fullCalendar.addEntryClickedListener(event -> {
            if (event.getEntry() instanceof DateEntry dateEntry) {
                UI.getCurrent().navigate(DateView.class, new RouteParameters(new RouteParam("date", dateEntry.getDate().getId())));
            }
        });
        fullCalendar.setPrefetchEnabled(true);
        fullCalendar.changeView(this.getCalendarView());
        return fullCalendar;
    }

    protected abstract org.vaadin.stefan.fullcalendar.CalendarView getCalendarView();

    public Component getCalendarPage(int page, String search) {
        CallbackEntryProvider<Entry> entryProvider = EntryProvider.fromCallbacks(
                query -> dateRepository.findByStartBetweenAndTitleLikeAndGroupMembersInAndGroupOrganisationMembersIn(query.getStart(), query.getEnd(), search, this.person).map(DateEntry::new),
                entryId -> dateRepository.findById(Long.valueOf(entryId)).map(DateEntry::new).orElse(null)
        );
        FullCalendar fullCalendar = setupCalendar();
        fullCalendar.setEntryProvider(entryProvider);
        fullCalendar.gotoDate(this.getStart(page));
        fullCalendar.getEntryProvider().refreshAll();
        return fullCalendar;
    }

    protected abstract LocalDate getStart(int page);

    public static class Month extends CalendarPageView {

        public Month(DateRepository dateRepository, GroupRepository groupRepository, OrganisationRepository organisationRepository, Person principal) {
            super(dateRepository, groupRepository, organisationRepository, principal);
        }

        @Override
        protected org.vaadin.stefan.fullcalendar.CalendarView getCalendarView() {
            return CalendarViewImpl.DAY_GRID_MONTH;
        }

        @Override
        protected LocalDate getStart(int page) {
            return LocalDate.now(ZoneOffset.UTC).plusMonths(page).withDayOfMonth(1);
        }

        @Override
        public String getTitle(int page, Locale locale) {
            return getStart(page).format(DateTimeFormatter.ofPattern("MMMM yyyy").withLocale(locale));
        }

        @Override
        public int getPage(Date date, String search) {
            return (int) LocalDate.now().withDayOfMonth(1).until(date.getStart().withDayOfMonth(1), ChronoUnit.MONTHS);
        }
    }

    public static class Year extends CalendarPageView {

        public Year(DateRepository dateRepository, GroupRepository groupRepository, OrganisationRepository organisationRepository, Person principal) {
            super(dateRepository, groupRepository, organisationRepository, principal);
        }

        @Override
        protected org.vaadin.stefan.fullcalendar.CalendarView getCalendarView() {
            return CalendarViewImpl.MULTI_MONTH;
        }

        @Override
        protected LocalDate getStart(int page) {
            return LocalDate.now(ZoneOffset.UTC).plusYears(page);
        }

        @Override
        public String getTitle(int page, Locale locale) {
            return getStart(page).format(DateTimeFormatter.ofPattern("yyyy").withLocale(locale));
        }

        @Override
        public int getPage(Date date, String search) {
            return (int) LocalDate.now().withDayOfYear(1).until(date.getStart().withDayOfYear(1), ChronoUnit.YEARS);
        }
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
}
