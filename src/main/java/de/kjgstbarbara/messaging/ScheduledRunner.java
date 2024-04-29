package de.kjgstbarbara.messaging;

import de.kjgstbarbara.data.*;
import de.kjgstbarbara.service.DatesService;
import de.kjgstbarbara.service.PersonsService;
import de.kjgstbarbara.service.ReminderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
    @Autowired
    private ReminderService reminderService;
    @Autowired
    private SenderUtils messageSender;

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
            for (Person p : personsService.getPersonsRepository().findAll()) {
                Feedback.Status feedback = d.getStatusFor(p);
                if (d.getPollScheduledFor().equals(LocalDate.now())
                        && d.isPollRunning()
                        && d.getBoard().getMembers().contains(p)
                        && now.getHour() == p.getRemindMeTime()) {
                    messageSender.sendDatePoll(d, p, false);
                }

                if (d.getBoard().getMembers().contains(p) && !feedback.equals(Feedback.Status.OUT)) {
                    // TODO Wann soll jemand diese Nachricht bekommen?
                    //  Nur wenn man zugesagt hat oder auch bei keiner Rückmeldung?
                    //  Oder wenn jemand spontan vielleicht doch kommt?
                    for(Reminder reminder : reminderService.getReminders(p)) {
                        if(reminder.matches(d.getStart(), p.getRemindMeTime())) {
                            messageSender.sendMessageFormatted(DATE_REMINDER_TEMPLATE, p, d, false);
                        }
                    }
                }
            }
        }

        // Neue Termine?
    }

    private static final String DATE_REMINDER_TEMPLATE =
            """
            Hey #PERSON_FIRSTNAME,
            du hast %s einen Termin bei der KjG :)
            #DATE_TITLE (#BOARD_TITLE)
            #DATE_START_DATE von #DATE_START_TIME
            #DATE_END_DATE bis #DATE_END_TIME
            Weitere Informationen zu diesem Termin findest du unter: #DATE_LINK
            """;
}
