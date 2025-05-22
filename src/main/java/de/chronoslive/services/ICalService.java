package de.chronoslive.services;

import biweekly.ICalendar;
import biweekly.component.VEvent;
import biweekly.property.Attendee;
import de.chronoslive.entitys.Date;
import de.chronoslive.entitys.Feedback;
import de.chronoslive.entitys.Person;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Service
public class ICalService {
    private final DatesService datesService;

    public ICalService(DatesService datesService) {
        this.datesService = datesService;
    }

    public ICalendar getCalendarByGroup(long groupId) {
        ICalendar calendar = new ICalendar();

        List<Date> events = this.datesService.getDateRepository().findByGroupId(groupId);

        for (Date event : events) {
            VEvent vEvent = dateToVEvent(event);

            calendar.addEvent(vEvent);
        }

        return calendar;
    }

    public ICalendar getCalendarByOrganisation(long organisationId) {
        ICalendar calendar = new ICalendar();

        List<Date> events = this.datesService.getDateRepository().findByGroupOrganisationId(organisationId);

        for (Date event : events) {
            VEvent vEvent = dateToVEvent(event);

            calendar.addEvent(vEvent);
        }

        return calendar;
    }

    public ICalendar getCalendarByDate(long dateId) {
        ICalendar calendar = new ICalendar();

        Optional<Date> event = this.datesService.getDateRepository().findById(dateId);

        if (event.isPresent()) {
            VEvent vEvent = dateToVEvent(event.get());

            calendar.addEvent(vEvent);
        }

        return calendar;
    }

    public ICalendar getCalendarByYear(int year) {
        ICalendar calendar = new ICalendar();

        LocalDateTime yearStart = LocalDateTime.of(year, 1, 1, 0, 0);
        long time = System.currentTimeMillis();
        List<Date> events = this.datesService.getDateRepository().findByStartBeforeAndEndAfter(yearStart.withYear(year + 1), yearStart);
        System.out.println(System.currentTimeMillis() - time);

        for (Date event : events) {
            VEvent vEvent = dateToVEvent(event);

            calendar.addEvent(vEvent);
        }

        return calendar;
    }

    public ICalendar getCalendarByPrincipal(Person person) {
        ICalendar calendar = new ICalendar();

        List<Date> events = this.datesService.getDateRepository().findByGroupMembersInAndGroupOrganisationMembersIn(List.of(person), List.of(person));

        for (Date event : events) {
            VEvent vEvent = dateToVEvent(event);

            calendar.addEvent(vEvent);
        }

        return calendar;
    }

    public ICalendar getPublicCalendar() {
        ICalendar calendar = new ICalendar();

        //List<Date> events = this.datesService.getDateRepository().findByPublish(true);
        List<Date> events = this.datesService.getDateRepository().findAll();

        for (Date event : events) {
            VEvent vEvent = dateToVEvent(event);

            calendar.addEvent(vEvent);
        }

        return calendar;
    }

    public ICalendar getPublicCalendarByOrganisation(long organisationId) {
        ICalendar calendar = new ICalendar();

        //List<Date> events = this.datesService.getDateRepository().findByPublishAndGroupOrganisationId(true, organisationId);
        List<Date> events = this.datesService.getDateRepository().findByGroupOrganisationId(organisationId);

        for (Date event : events) {
            VEvent vEvent = dateToVEvent(event);

            calendar.addEvent(vEvent);
        }

        return calendar;
    }

    private VEvent dateToVEvent(Date event) {
        VEvent vEvent = new VEvent();
        vEvent.setUid(String.valueOf(event.getId()));

        vEvent.setSummary(event.getTitle());

        vEvent.setDateStart(java.util.Date.from(event.getStart().atZone(ZoneOffset.UTC).toInstant()));
        vEvent.setDateEnd(java.util.Date.from(event.getEnd().atZone(ZoneOffset.UTC).toInstant()));

        vEvent.addRelatedTo(String.valueOf(event.getLinkedTo()));

        vEvent.setDescription(event.getNotes());

        vEvent.setLocation(event.getVenue());

        for (Person member : event.getGroup().getMembers()) {
            if (event.getStatusFor(member).equals(Feedback.Status.COMMITTED)) {
                vEvent.addAttendee(new Attendee(member.getName(), member.getEMailAddress()));
            }
        }
        return vEvent;
    }
}
