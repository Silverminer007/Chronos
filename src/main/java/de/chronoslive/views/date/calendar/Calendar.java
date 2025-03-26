package de.chronoslive.views.date.calendar;

import com.vaadin.flow.component.Component;
import de.chronoslive.entitys.Date;

import java.util.Locale;

public interface Calendar {
    Component getCalendarPage(int page, String search);
    String getTitle(int page, Locale locale);
    int getPage(Date date, String search);
}
