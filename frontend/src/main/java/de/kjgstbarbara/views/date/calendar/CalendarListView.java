package de.kjgstbarbara.views.date.calendar;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.H6;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.RouteParam;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.kjgstbarbara.components.FeedbackButton;
import de.kjgstbarbara.data.Date;
import de.kjgstbarbara.data.Feedback;
import de.kjgstbarbara.data.Person;
import de.kjgstbarbara.service.DateRepository;
import de.kjgstbarbara.service.FeedbackRepository;
import de.kjgstbarbara.views.date.DateView;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public abstract class CalendarListView implements Calendar {
    protected final DateRepository dateRepository;
    protected final FeedbackRepository feedbackRepository;
    protected final Person person;

    public CalendarListView(DateRepository dateRepository, FeedbackRepository feedbackRepository, Person person) {
        this.dateRepository = dateRepository;
        this.feedbackRepository = feedbackRepository;
        this.person = person;
    }

    public Component getCalendarPage(int page, String search) {
        VerticalLayout dateListLayout = new VerticalLayout();
        List<Date> dates = this.findSubListOfDates(search, page);

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
            dateEntry.setAlignItems(FlexComponent.Alignment.CENTER);
            dateEntry.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
            dateEntry.addClassNames(LumoUtility.Background.TINT_5, LumoUtility.BorderRadius.SMALL);

            HorizontalLayout dateInformation = new HorizontalLayout();
            dateInformation.setWidthFull();
            dateInformation.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
            dateInformation.setAlignItems(FlexComponent.Alignment.CENTER);
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
            feedbackButtons.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
            feedbackButtons.setAlignItems(FlexComponent.Alignment.CENTER);

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
            if (search != null) {
                dateListLayout.add(new H6("Keine Termine gefunden"));
            } else {
                dateListLayout.add(new H6("Bisher keine Termine zu sehen"));
            }
        }
        return dateListLayout;
    }

    protected abstract List<Date> findSubListOfDates(String search, int page);

    public static class Amount extends CalendarListView {

        public Amount(DateRepository dateRepository, FeedbackRepository feedbackRepository, Person person) {
            super(dateRepository, feedbackRepository, person);
        }

        @Override
        public String getTitle(int page, Locale locale) {
            if (page < 0) {
                int intervalStart = ((page * -1) - 1) * 20 + 1;
                return  "Vergangenheit " + intervalStart + " - " + (intervalStart + 19);
            } else {
                int intervalStart = page * 20 + 1;
                return  "Zukunft " + intervalStart + " - " + (intervalStart + 19);
            }
        }

        protected List<Date> findSubListOfDates(String search, int page) {
            return this.dateRepository.calendarQuery(search, page, this.person);
        }
    }

    public static class Month extends CalendarListView {

        public Month(DateRepository dateRepository, FeedbackRepository feedbackRepository, Person person) {
            super(dateRepository, feedbackRepository, person);
        }

        @Override
        public String getTitle(int page, Locale locale) {
            return LocalDate.now(ZoneOffset.UTC).plusMonths(page).format(DateTimeFormatter.ofPattern("MMMM yyyy").withLocale(locale));
        }

        protected List<Date> findSubListOfDates(String search, int page) {
            return dateRepository.findByStartBetweenAndTitleLikeAndGroupMembersInAndGroupOrganisationMembersIn(
                            LocalDateTime.now(ZoneOffset.UTC)
                                    .plusMonths(page).withDayOfMonth(1).withHour(0).withMinute(0),
                            LocalDateTime.now(ZoneOffset.UTC)
                                    .plusMonths(1 + page).withDayOfMonth(1).minusDays(1).withHour(0).withMinute(0),
                            search,
                            this.person)
                    .sorted().toList();
        }
    }
}
