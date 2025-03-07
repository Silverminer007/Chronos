package de.henzeob.chronos.services;

import de.henzeob.chronos.exceptions.CaldavException;
import org.springframework.stereotype.Service;
import org.w3c.dom.Node;

import java.util.List;

@Service
public class CaldavService {
    private final DateService dateService;
    private final FindDatesService findDatesService;

    public CaldavService(DateService dateService, FindDatesService findDatesService) {
        this.dateService = dateService;
        this.findDatesService = findDatesService;
    }

    public String findProperties(String requestBody) throws CaldavException {
        return null;
    }

    public String patchProperties(String requestBody) throws CaldavException {
        return null;
    }

    public String findPropertiesForDate(String requestBody, String dateId) throws CaldavException {
        return null;
    }

    public String patchPropertiesForDate(String requestBody, String dateId) throws CaldavException {
        return null;
    }

    public String calendarQueryReport(List<String> propertyNames, Node filterNode) throws CaldavException {
        return null;
    }

    public String calendarMultiGetReport(List<String> urls, List<String> propertyNames) {
        // Extract preconditions from xml
        // read properties from xml
        // load dates for specified URLs
        // check preconditions for each date
        // populate response with properties
        return null;
    }

    public String freeBusyQueryReport(String timeRangeStart, String timeRangeEnd) {
        // Read time range from xml
        // read dates between time range
        // build free busy object
        // return free busy object
        return null;
    }
}
