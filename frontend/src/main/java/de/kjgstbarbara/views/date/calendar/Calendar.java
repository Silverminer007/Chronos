package de.kjgstbarbara.views.date.calendar;

import com.vaadin.flow.component.Component;

import java.util.Locale;

public interface Calendar {
    Component getCalendarPage(int page, String search);
    String getTitle(int page, Locale locale);
}
