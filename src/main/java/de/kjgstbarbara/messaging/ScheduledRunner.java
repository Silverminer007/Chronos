package de.kjgstbarbara.messaging;

import de.kjgstbarbara.FriendlyError;
import de.kjgstbarbara.data.*;
import de.kjgstbarbara.service.DatesService;
import de.kjgstbarbara.service.PersonsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import static java.util.concurrent.TimeUnit.*;

@Component
public class ScheduledRunner implements CommandLineRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledRunner.class);

    private static final ScheduledExecutorService SCHEDULER =
            Executors.newScheduledThreadPool(1);

    @Autowired
    private PersonsService personsService;
    @Autowired
    private DatesService datesService;

    @Override
    public void run(String... args) {
        LOGGER.info("Hourly runner setup");

        final ScheduledFuture<?> repeatingTask =
                SCHEDULER.scheduleAtFixedRate(this::runTasks, 0, 1, MINUTES);
        SCHEDULER.schedule(() -> {
            repeatingTask.cancel(true);
        }, 60 * 60, SECONDS);
    }

    /**
     * Runs every 60 Minutes
     */
    private void runTasks() {
        LocalDateTime now = LocalDateTime.now();
        // Terminerinnerung → Im Profil Erinnerungen erstellen (in welchen Abständen) → Standard 1 Tag vorher, immer 19 Uhr gesammelt
        for (Date d : datesService.getDateRepository().findAll()) {
            for (Person p : d.getGroup().getMembers()) {
                Feedback.Status feedback = d.getStatusFor(p);
                if (!feedback.equals(Feedback.Status.CANCELLED)) {
                    // TODO Wann soll jemand diese Nachricht bekommen?
                    //  Nur wenn man zugesagt hat oder auch bei keiner Rückmeldung?
                    //  Oder wenn jemand spontan vielleicht doch kommt?
                    if ((p.getRemindMeTime().contains(now.getHour()) || p.getDayReminderIntervals().contains((int) now.until(d.getStart(), ChronoUnit.DAYS)))
                            || p.getHourReminderIntervals().contains((int) now.until(d.getStart(), ChronoUnit.HOURS))) {
                        try {
                            d.getGroup().getOrganisation().sendMessageTo(new MessageFormatter().person(p).date(d).format(DATE_REMINDER_TEMPLATE), p);
                        } catch (FriendlyError e) {
                            throw new RuntimeException(e);// TODO Show to handle invalid phone numbers
                        }
                    }
                }

                if (LocalDate.now().isEqual(d.getPollScheduledFor())
                        && d.isPollRunning()
                        && p.getRemindMeTime().contains(now.getHour())) {
                    try {
                        d.getGroup().getOrganisation().sendDatePoll(d, p);
                    } catch (FriendlyError e) {
                        LOGGER.error("Die geplante Terminabfrage konnte nicht verschickt werden", e);
                    }
                }
            }
        }

        // Neue Termine?
    }

    private static final String DATE_REMINDER_TEMPLATE =
            """
                    Hey #PERSON_FIRSTNAME,
                    du hast #DATE_TIME_UNTIL_START einen Termin bei der KjG :)
                    #DATE_TITLE (#BOARD_TITLE)
                    Von #DATE_START_TIME am #DATE_START_DATE
                    Bis #DATE_END_TIME am #DATE_END_DATE
                                       \s
                    Deine Rückmeldung zu diesem Termin: #FEEDBACK_STATUS""";//Weitere Informationen zu diesem Termin findest du unter: #DATE_LINK
}
