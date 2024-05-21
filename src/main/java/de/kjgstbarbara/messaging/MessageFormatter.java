package de.kjgstbarbara.messaging;

import de.kjgstbarbara.data.*;
import jakarta.annotation.Nullable;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

@Setter
@Accessors(fluent = true)
public class MessageFormatter {
    private Organisation organisation;
    private Date date;
    private Person person;
    private Group group;
    private Feedback.Status feedback;

    public static String placeholders(String input, @Nullable Organisation organisation, @Nullable Date date, @Nullable Person person, @Nullable Group group, @Nullable Feedback.Status feedback) {
        String output = input;
        if(date != null) {
            output = datePlaceholder(output, date);
            if(group == null) {
                group = date.getGroup();
            }
            output = boardPlaceholders(output, group);
            if(organisation == null) {
                organisation = group.getOrganisation();
            }
            output = organisationPlaceholders(output, organisation);
        }
        if(person != null) {
            output = personPlaceholder(output, person);
        }
        if(feedback == null && date != null && person != null) {
            feedback = date.getStatusFor(person);
        }
        if(feedback != null) {
            output = feedbackPlaceholder(output, feedback);
        }
        return output;
    }

    public String format(String input) {
        return placeholders(input, organisation, date, person, group, feedback);
    }

    private static String datePlaceholder(String input, Date date) {
        String output = input;
        // DATE_TITLE
        String dateTitle = date.getTitle();
        output = output.replaceAll("#DATE_TITLE", dateTitle);
        // DATE_START_DATE
        String startDate = date.getStart().format(DateTimeFormatter.ofPattern("d MMM uuuu", Locale.GERMAN));
        output = output.replaceAll("#DATE_START_DATE", startDate);
        // DATE_START_TIME
        String startTime = date.getStart().format(DateTimeFormatter.ofPattern("hh:mm", Locale.GERMAN)) + " Uhr";
        output = output.replaceAll("#DATE_START_TIME", startTime);
        // DATE_END_DATE
        String endDate = date.getEnd().format(DateTimeFormatter.ofPattern("d MMM uuuu", Locale.GERMAN));
        output = output.replaceAll("#DATE_END_DATE", endDate);
        // DATE_END_TIME
        String endTime = date.getStart().format(DateTimeFormatter.ofPattern("hh:mm", Locale.GERMAN)) + " Uhr";
        output = output.replaceAll("#DATE_END_TIME", endTime);
        // DATE_POLL_DATE
        if(date.getPollScheduledFor() != null) {
            String pollDate = date.getPollScheduledFor().format(DateTimeFormatter.ofPattern("d MMM uuuu", Locale.GERMAN));
            output = output.replaceAll("#DATE_POLL_DATE", pollDate);
        }
        // DATE_TIME_UNTIL_START
        LocalDateTime now = LocalDateTime.now();
        long daysUntilStart = now.until(date.getStart(), ChronoUnit.DAYS);
        long hoursUntilStart = now.until(date.getStart(), ChronoUnit.HOURS);
        String timeUntilStart = switch ((int) daysUntilStart) {
            case 0 -> hoursUntilStart == 1 ? "in einer Stunde" : "in " + hoursUntilStart + " Stunden";
            case 1 -> "morgen";
            case 2 -> "übermorgen";
            case 7 -> "in einer Woche";
            default -> "in " + daysUntilStart + " Tagen";
        };
        output = output.replaceAll("#DATE_TIME_UNTIL_START", timeUntilStart);
        // DATE_LINK
        String link = "";// TODO
        output = output.replaceAll("#DATE_LINK", link);
        return output;
    }

    private static String personPlaceholder(String input, Person person) {
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
        if(person.getResetTokenExpires() != null) {
            long hoursUntilExpired = person.getResetTokenExpires().until(LocalDateTime.now(), ChronoUnit.HOURS);
            String resetExpiresIn = String.valueOf(hoursUntilExpired);
            output = output.replaceAll("#PERSON_RESET_EXPIRES_IN", resetExpiresIn);
        }
        return output;
    }

    private static String boardPlaceholders(String input, Group group) {
        String output = input;
        // BOARD_TITLE
        String title = group.getName();
        output = output.replaceAll("#BOARD_TITLE", title);
        return output;
    }

    private static String feedbackPlaceholder(String input, Feedback.Status feedback) {
        String output = input;
        // FEEDBACK_STATUS
        String status = feedback.getReadable();
        output = output.replaceAll("#FEEDBACK_STATUS", status);
        return output;
    }

    private static String organisationPlaceholders(String input, Organisation organisation) {
        String output = input;
        // ORGANISATION_NAME
        String name = organisation.getName();
        output = output.replaceAll("#ORGANISATION_NAME", name);
        return output;
    }
}