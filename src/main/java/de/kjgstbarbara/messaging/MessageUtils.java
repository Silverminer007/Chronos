package de.kjgstbarbara.messaging;

import de.kjgstbarbara.FriendlyError;
import de.kjgstbarbara.data.Date;
import de.kjgstbarbara.data.Feedback;
import de.kjgstbarbara.data.PasswordReset;
import de.kjgstbarbara.data.Person;
import de.kjgstbarbara.security.SecurityUtils;
import de.kjgstbarbara.service.DatesService;
import de.kjgstbarbara.service.FeedbackService;
import de.kjgstbarbara.service.PersonsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import static java.util.concurrent.TimeUnit.*;

@Component
public class MessageUtils implements CommandLineRunner {
    private static final IMessageSender MESSAGE_SENDER = new MultiMessageSender(new WhatsAppMessageSender(4915752657194L));

    private static final ScheduledExecutorService SCHEDULER =
            Executors.newScheduledThreadPool(1);

    public static final String RESET = "Hey %s %s,%ndu hast angefragt dein Passwort zurückzusetzen." +
            "%nWenn du das nicht getan hast klicke auf keinen Fall auf den Link unten, " +
            "sondern lösche diese Nachricht." +
            "%nUm dein Passwort zurückzusetzen, klicke auf diesen Link:" +
            "%nhttp://%s/password-reset/%s";

    public static final String CREATED = "Hey %s %s,%nfür dich wurde ein neuer Account im Termintool der KjG angelegt.%n" +
            "Um deinen Account zu aktivieren klicke auf diesen Link:%n" +
            "http://%s/password-reset/%s";

    public static PasswordReset sendPasswordResetMessage(Person requester, String messageFormat) throws FriendlyError {
        PasswordReset passwordReset = new PasswordReset();
        passwordReset.setRequester(requester);
        String token;
        try {
            token = SecurityUtils.generatePassword(200);
        } catch (NoSuchAlgorithmException e) {
            throw new FriendlyError(e.getMessage());
        }
        passwordReset.setToken(token);
        String message = String.format(messageFormat, requester.getFirstName(), requester.getLastName(), "localhost:8080", token);
        MESSAGE_SENDER.sendMessage(message, requester);
        return passwordReset;
    }

    @Override
    public void run(String... args) {
        System.out.println("Message Sender Setup started");
        final ScheduledFuture<?> repeatingTask =
                SCHEDULER.scheduleAtFixedRate(this::runTasks, 0, 1, MINUTES);
        SCHEDULER.schedule(() -> {
            repeatingTask.cancel(true);
        }, 60 * 60, SECONDS);
    }

    @Autowired
    private PersonsService personsService;
    @Autowired
    private DatesService datesService;
    @Autowired
    private FeedbackService feedbackService;

    /**
     * Runs every 60 Minutes
     */
    private void runTasks() {
        LocalDateTime now = LocalDateTime.now();
        // Terminerinnerung -> Im Profil Erinnerungen erstellen (in welchen Abständen) -> Standard 1 Tag vorher, immer 19 Uhr gesammelt
        for (Person p : personsService.getPersonsRepository().findAll()) {
            for (Date d : datesService.getDateRepository().findAll()) {
                Feedback.Status feedback = feedbackService.getFeedbackRepository().findById(Feedback.Key.create(p, d)).map(Feedback::getStatus).orElse(Feedback.Status.DONTKNOW);
                if (d.getBoard().getMembers().contains(p) && !feedback.equals(Feedback.Status.OUT)) {// TODO Wann soll jemand diese Nachricht bekommen? Nur wenn man zugesagt hat oder auch bei keiner Rückmeldung? Oder wenn jemand spontan vielleicht doch kommt?
                    if (p.isRemindOneHourBefore() && now.until(d.getStart(), ChronoUnit.HOURS) == 1) {
                        sendDateReminder(p, d, "in einer Stunde");
                        continue;
                    }
                    if (p.isRemindTwoHoursBefore() && now.until(d.getStart(), ChronoUnit.HOURS) == 2) {
                        sendDateReminder(p, d, "in 2 Stunden");
                        continue;
                    }
                    if (p.isRemindSixHoursBefore() && now.until(d.getStart(), ChronoUnit.HOURS) == 6) {
                        sendDateReminder(p, d, "in 6 Stunden");
                        continue;
                    }
                    if (now.getHour() == p.getRemindMeTime()) {
                        if (p.isRemindOneHourBefore() && now.until(d.getStart(), ChronoUnit.DAYS) == 1) {
                            sendDateReminder(p, d, "morgen");
                            continue;
                        }
                        if (p.isRemindTwoDaysBefore() && now.until(d.getStart(), ChronoUnit.DAYS) == 2) {
                            sendDateReminder(p, d, "in zwei Tagen");
                            continue;
                        }
                        if (p.isRemindOneWeekBefore() && now.until(d.getStart(), ChronoUnit.DAYS) == 7) {
                            sendDateReminder(p, d, "in einer Woche");
                            continue;
                        }
                        if (p.isMonthOverview() && now.getDayOfMonth() == 25) {
                            sendDateReminder(p, d, "diesen Monat");
                        }
                    }
                }
            }
        }

        // Terminabfragen planen für XY Uhr

        // Monatsübersicht am 01. eines Monats für die nächsten 5 Wochen (optional)

        // Neue Termine?
    }

    private static void sendDateReminder(Person sendTo, Date date, String distance) {
        String format =
                """
                        Hey %s,
                        du hast %s einen Termin bei der KjG :)
                                                
                        %s (%s)
                        Von %s Uhr
                        Bis %s Uhr
                                                
                        Weitere Informationen zu diesem Termin findest du unter: %s
                        """;
        /*
        1. Vorname
        2. Abstand
        3. Date Titel
        4. Board Titel
        5. Start
        6. End
        7. Terminlink
         */
        String start = date.getStart().format(DateTimeFormatter.ofPattern("d.MM.yyyy hh:mm"));
        String end = date.getEnd() == null ? "" : date.getEnd().format(DateTimeFormatter.ofPattern("d.MM.yyyy hh:mm"));
        String message = String.format(format,
                sendTo.getFirstName(),
                distance,
                date.getBoard().getTitle(),
                date.getTitle(),
                start,
                end,
                "");
        try {
            MESSAGE_SENDER.sendMessage(message, sendTo);
        } catch (FriendlyError e) {
            // TODO Error Logging
            System.out.println(e);
        }
    }

    // Termin direkt abfragen
    public static void sendDatePoll(Date date) {

    }

    // Protokoll hochladen

    // Protokoll abfragen
}
