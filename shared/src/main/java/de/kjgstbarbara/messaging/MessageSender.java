package de.kjgstbarbara.messaging;

import de.kjgstbarbara.Result;
import de.kjgstbarbara.data.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MessageSender {
    private final List<Context> contexts = new ArrayList<>();
    private final Person sendTo;

    public MessageSender(Person sendTo) {
        this.sendTo = sendTo;
    }

    public Result send(String message) {
        return this.send(message, this.sendTo.getPrefferedPlatform());
    }

    public Result send(String message, Platform platform) {
        String formattedMessage = this.format(message);
        return platform.send(this.sendTo, formattedMessage);
    }

    public MessageSender person(Person person) {
        return this.person(person, "PERSON");
    }

    public MessageSender person(Person person, String name) {
        this.contexts.add(new PersonContext(person, name));
        return this;
    }

    public MessageSender organisation(Organisation organisation) {
        return this.organisation(organisation, "ORGANISATION");
    }

    public MessageSender organisation(Organisation organisation, String name) {
        this.contexts.add(new OrganisationContext(organisation, name));
        return this;
    }

    public MessageSender date(Date date) {
        return this.date(date, "DATE");
    }

    public MessageSender date(Date date, String name) {
        this.contexts.add(new DateContext(date, name));
        return this;
    }

    public MessageSender group(Group group) {
        return this.group(group, "GROUP");
    }

    public MessageSender group(Group group, String name) {
        this.contexts.add(new GroupContext(group, name));
        return this;
    }

    public MessageSender feedback(Feedback.Status feedback) {
        return this.feedback(feedback, "FEEDBACK");
    }

    public MessageSender feedback(Feedback.Status feedback, String name) {
        this.contexts.add(new FeedbackContext(feedback, name));
        return this;
    }


    private String format(String input) {
        String output = input;
        for (Context context : this.contexts) {
            output = context.process(output);
        }
        // BASE_URL
        output = output.replaceAll("#BASE_URL", System.getenv("HOST_DOMAIN"));
        return output;
    }

    private interface Context {
        String process(String string);
    }

    private record PersonContext(Person person, String name) implements Context {

        @Override
        public String process(String string) {
            String output = string;
            // PERSON_NAME
            String name = person.getName();
            output = output.replaceAll("#" + name() + "_NAME", name);
            // PERSON_FIRSTNAME
            String firstName = person.getFirstName();
            output = output.replaceAll("#" + name() + "_FIRSTNAME", firstName);
            // PERSON_LASTNAME
            String lastName = person.getLastName();
            output = output.replaceAll("#" + name() + "_LASTNAME", lastName);
            // PERSON_USERNAME
            String username = person.getUsername();
            output = output.replaceAll("#" + name() + "_USERNAME", username);
            // PERSON_PHONE_NUMBER
            String phoneNumber = "+" + person.getPhoneNumber();
            output = output.replaceAll("#" + name() + "_PHONE_NUMBER", phoneNumber);
            // PERSON_E_MAIL
            String eMail = person.getEMailAddress();
            output = output.replaceAll("#" + name() + "_E_MAIL", eMail);
            // PERSON_ID
            output = output.replaceAll("#" + name() + "_ID", String.valueOf(person.getId()));
            return output;
        }
    }

    private record OrganisationContext(Organisation organisation, String name) implements Context {

        @Override
        public String process(String string) {
            String output = string;
            // ORGANISATION_ID
            String id = String.valueOf(organisation.getId());
            output = output.replaceAll("#" + name + "_ID", id);
            // ORGANISATION_NAME
            String name = organisation.getName();
            output = output.replaceAll("#" + name + "_NAME", name);
            // ORGANISATION_ADMIN_NAME
            String adminName = organisation.getAdmin().getName();
            output = output.replaceAll("#" + name + "_ADMIN_NAME", adminName);
            return output;
        }
    }

    private record DateContext(Date date, String name) implements Context {

        @Override
        public String process(String string) {
            String output = string;
            // DATE_TITLE
            String dateTitle = date.getTitle();
            output = output.replaceAll("#" + name + "_TITLE", dateTitle);
            // DATE_START_DATE
            String startDate = date.getStart().format(DateTimeFormatter.ofPattern("d MMM uuuu", Locale.GERMAN));
            output = output.replaceAll("#" + name + "_START_DATE", startDate);
            // DATE_START_TIME
            String startTime = date.getStart().format(DateTimeFormatter.ofPattern("HH:mm", Locale.GERMAN)) + " Uhr";
            output = output.replaceAll("#" + name + "_START_TIME", startTime);
            // DATE_END_DATE
            String endDate = date.getEnd().format(DateTimeFormatter.ofPattern("d MMM uuuu", Locale.GERMAN));
            output = output.replaceAll("#" + name + "_END_DATE", endDate);
            // DATE_END_TIME
            String endTime = date.getStart().format(DateTimeFormatter.ofPattern("HH:mm", Locale.GERMAN)) + " Uhr";
            output = output.replaceAll("#" + name + "_END_TIME", endTime);
            // DATE_POLL_DATE
            if (date.getPollScheduledFor() != null) {
                String pollDate = date.getPollScheduledFor().format(DateTimeFormatter.ofPattern("d MMM uuuu", Locale.GERMAN));
                output = output.replaceAll("#" + name + "_POLL_DATE", pollDate);
            }
            // DATE_TIME_UNTIL_START
            LocalDateTime now = LocalDateTime.now();
            String timeUntilStart = getTimeUntilStart(now);
            output = output.replaceAll("#DATE_TIME_UNTIL_START", timeUntilStart);
            // DATE_LINK
            String link = System.getenv("HOST_DOMAIN") + "/date/" + date.getId();
            output = output.replaceAll("#DATE_LINK", link);
            // DATE_ID
            output = output.replaceAll("#DATE_ID", String.valueOf(date.getId()));
            return output;
        }

        private String getTimeUntilStart(LocalDateTime now) {
            long daysUntilStart = now.until(date.getStart(), ChronoUnit.DAYS);
            long hoursUntilStart = now.until(date.getStart(), ChronoUnit.HOURS);
            return switch ((int) daysUntilStart) {
                case 0 -> hoursUntilStart == 1 ? "in einer Stunde" : "in " + hoursUntilStart + " Stunden";
                case 1 -> "morgen";
                case 2 -> "übermorgen";
                case 7 -> "in einer Woche";
                default -> "in " + daysUntilStart + " Tagen";
            };
        }
    }

    private record GroupContext(Group group, String name) implements Context {

        @Override
        public String process(String string) {
            String output = string;
            // BOARD_TITLE
            String title = group.getName();
            output = output.replaceAll("#" + name + "_NAME", title);
            return output;
        }
    }

    private record FeedbackContext(Feedback.Status feedback, String name) implements Context {

        @Override
        public String process(String string) {
            String output = string;
            // FEEDBACK_STATUS
            String status = feedback.getReadable();
            output = output.replaceAll("#" + name() + "_STATUS", status);
            return output;
        }
    }
}