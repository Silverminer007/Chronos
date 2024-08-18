package de.kjgstbarbara.chronos.messaging;

import de.kjgstbarbara.chronos.data.*;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Properties;

@Setter
@Accessors(fluent = true)
public class MessageFormatter {
    private Organisation organisation;
    private Date date;
    private Person person;
    private Group group;
    private Feedback.Status feedback;
    private String baseURL = "";

    public String format(String input) {
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream("chronos.properties"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (baseURL.isBlank()) {
            baseURL = properties.getProperty("chronos.base-url");
        }
        String output = input;
        if (date != null) {
            output = datePlaceholder(output);
            if (group == null) {
                group = date.getGroup();
            }
            if (organisation == null) {
                organisation = group.getOrganisation();
            }
        }
        if (group != null) {
            output = groupPlaceholder(output);
        }
        if (organisation != null) {
            output = organisationPlaceholders(output);
        }
        if (person != null) {
            output = personPlaceholder(output);
        }
        if (feedback == null && date != null && person != null) {
            feedback = date.getStatusFor(person);
        }
        if (feedback != null) {
            output = feedbackPlaceholder(output);
        }
        // BASE_URL
        output = output.replaceAll("#BASE_URL", baseURL);
        return output;
    }

    private String datePlaceholder(String input) {
        String output = input;
        // DATE_TITLE
        String dateTitle = date.getTitle();
        output = output.replaceAll("#DATE_TITLE", dateTitle);
        // DATE_START_DATE
        String startDate = date.getStartAtTimezone(this.person.getTimezone()).format(DateTimeFormatter.ofPattern("d MMM uuuu", Locale.GERMAN));
        output = output.replaceAll("#DATE_START_DATE", startDate);
        // DATE_START_TIME
        String startTime = date.getStartAtTimezone(this.person.getTimezone()).format(DateTimeFormatter.ofPattern("HH:mm", Locale.GERMAN)) + " Uhr";
        output = output.replaceAll("#DATE_START_TIME", startTime);
        // DATE_END_DATE
        String endDate = date.getEndAtTimezone(this.person.getTimezone()).format(DateTimeFormatter.ofPattern("d MMM uuuu", Locale.GERMAN));
        output = output.replaceAll("#DATE_END_DATE", endDate);
        // DATE_END_TIME
        String endTime = date.getStartAtTimezone(this.person.getTimezone()).format(DateTimeFormatter.ofPattern("HH:mm", Locale.GERMAN)) + " Uhr";
        output = output.replaceAll("#DATE_END_TIME", endTime);
        // DATE_POLL_DATE
        if (date.getPollScheduledFor() != null) {
            String pollDate = date.getPollScheduledFor().format(DateTimeFormatter.ofPattern("d MMM uuuu", Locale.GERMAN));
            output = output.replaceAll("#DATE_POLL_DATE", pollDate);
        }
        // DATE_TIME_UNTIL_START
        LocalDateTime now = LocalDateTime.now();
        long daysUntilStart = now.until(date.getStartAtTimezone(this.person.getTimezone()), ChronoUnit.DAYS);
        long hoursUntilStart = now.until(date.getStartAtTimezone(this.person.getTimezone()), ChronoUnit.HOURS);
        String timeUntilStart = switch ((int) daysUntilStart) {
            case 0 -> hoursUntilStart == 1 ? "in einer Stunde" : "in " + hoursUntilStart + " Stunden";
            case 1 -> "morgen";
            case 2 -> "übermorgen";
            case 7 -> "in einer Woche";
            default -> "in " + daysUntilStart + " Tagen";
        };
        output = output.replaceAll("#DATE_TIME_UNTIL_START", timeUntilStart);
        // DATE_LINK
        String link = baseURL + "/date/" + date.getId();
        output = output.replaceAll("#DATE_LINK", link);
        // DATE_ID
        output = output.replaceAll("#DATE_ID", String.valueOf(date.getId()));
        return output;
    }

    private String personPlaceholder(String input) {
        String output = input;
        // PERSON_NAME
        String name = person.getName();
        output = output.replaceAll("#PERSON_NAME", name);
        // PERSON_FIRSTNAME
        String firstName = person.getFirstName();
        output = output.replaceAll("#PERSON_FIRSTNAME", firstName);
        // PERSON_LASTNAME
        String lastName = person.getLastName();
        output = output.replaceAll("#PERSON_LASTNAME", lastName);
        // PERSON_USERNAME
        String username = person.getUsername();
        output = output.replaceAll("#PERSON_USERNAME", username);
        // PERSON_PHONE_NUMBER
        String phoneNumber = "+" + person.getPhoneNumber();
        output = output.replaceAll("#PERSON_PHONE_NUMBER", phoneNumber);
        // PERSON_E_MAIL
        String eMail = person.getEMailAddress();
        output = output.replaceAll("#PERSON_E_MAIL", eMail);
        // PERSON_RESET_TOKEN
        String resetToken = person.getResetToken();
        output = output.replaceAll("#PERSON_RESET_TOKEN", resetToken);
        // PERSON_RESET_LINK
        String resetLink = person.getResetToken();
        output = output.replaceAll("#PERSON_RESET_LINK", resetLink);
        // PERSON_RESET_EXPIRES_IN
        if (person.getResetTokenExpires() != null) {
            long hoursUntilExpired = person.getResetTokenExpires().until(LocalDateTime.now(), ChronoUnit.HOURS);
            String resetExpiresIn = String.valueOf(hoursUntilExpired);
            output = output.replaceAll("#PERSON_RESET_EXPIRES_IN", resetExpiresIn);
        }
        // PERSON_ID
        output = output.replaceAll("#PERSON_ID", String.valueOf(person.getId()));
        return output;
    }

    private String groupPlaceholder(String input) {
        String output = input;
        // BOARD_TITLE
        String title = group.getName();
        output = output.replaceAll("#BOARD_TITLE", title);
        return output;
    }

    private String feedbackPlaceholder(String input) {
        String output = input;
        // FEEDBACK_STATUS
        String status = feedback.getReadable();
        output = output.replaceAll("#FEEDBACK_STATUS", status);
        return output;
    }

    private String organisationPlaceholders(String input) {
        String output = input;
        // ORGANISATION_ID
        String id = String.valueOf(organisation.getId());
        output = output.replaceAll("#ORGANISATION_ID", id);
        // ORGANISATION_NAME
        String name = organisation.getName();
        output = output.replaceAll("#ORGANISATION_NAME", name);
        // ORGANISATION_ADMIN_NAME
        String adminName = organisation.getAdmin().getName();
        output = output.replaceAll("#ORGANISATION_ADMIN_NAME", adminName);
        return output;
    }
}