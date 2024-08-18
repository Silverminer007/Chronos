package de.kjgstbarbara.chronos;

import biweekly.Biweekly;
import biweekly.ICalendar;
import biweekly.component.VEvent;
import biweekly.property.Color;
import biweekly.property.Summary;
import de.kjgstbarbara.chronos.data.Date;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class IcsHelper {
    public static String writeDateToIcs(Date... dates) {
        ICalendar iCalendar = new ICalendar();

        for (Date date : dates) {
            iCalendar.addEvent(dateToVEvent(date));
        }

        return Biweekly.write(iCalendar).go();
    }

    private static VEvent dateToVEvent(Date date) {
        VEvent event = new VEvent();
        Summary summary = new Summary(date.getTitle());
        event.setDateStart(java.util.Date.from(date.getStartAtTimezone(ZoneOffset.UTC).toInstant(ZoneOffset.UTC)));
        event.setDateEnd(java.util.Date.from(date.getEndAtTimezone(ZoneOffset.UTC).toInstant(ZoneOffset.UTC)));
        event.setSummary(summary);
        event.setColor(new Color(date.getGroup().getColor()));
        event.addComment(date.getNotes());
        event.setLocation(date.getVenue());
        return event;
    }
}
