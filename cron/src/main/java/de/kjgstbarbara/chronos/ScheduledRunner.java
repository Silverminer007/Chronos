package de.kjgstbarbara.chronos;

import de.kjgstbarbara.chronos.data.Date;
import de.kjgstbarbara.chronos.data.Feedback;
import de.kjgstbarbara.chronos.data.Person;
import de.kjgstbarbara.chronos.messaging.MessageFormatter;
import de.kjgstbarbara.chronos.service.DatesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;

@Component
public class ScheduledRunner implements CommandLineRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledRunner.class);

    @Autowired
    private DatesService datesService;

    @Override
    public void run(String... args) throws Exception {
        LocalDateTime now = LocalDateTime.now();
        LOGGER.info("------------------------------------------------------------------------------------------------");
        LOGGER.info("ERINNERUNGEN VERSCHICKEN {}: START", now.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")));
        try {
            // Terminerinnerung → Im Profil Erinnerungen erstellen (in welchen Abständen) → Standard 1 Tag vorher, immer 19 Uhr gesammelt
            for (Date d : datesService.getDateRepository().findByStartBetween(now, now.plusDays(8)).stream().sorted(Comparator.comparing(Date::getStart)).toList()) {
                LOGGER.info("Erinnerungen für {} am {} werden verschickt", d.getTitle(), d.getStart().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")));

                if (now.until(d.getStart(), ChronoUnit.HOURS) == 2) {
                    d.getGroup().getAdmins().forEach(admin -> {
                        LOGGER.info("Termin Infos für {} am {} werden an {} verschickt", d.getTitle(), d.getStart().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")), admin.getName());
                        StringBuilder summary = new StringBuilder();
                        summary.append("Hey ").append(admin.getFirstName()).append(",\n");
                        summary.append("gleich ist ").append(d.getTitle()).append("\n\n");
                        summary.append("Dabei sind:\n");
                        StringBuilder cancelled = new StringBuilder("Nicht dabei sind:\n");
                        StringBuilder noAnswer = new StringBuilder("Bisher nicht gemeldet haben sich:\n");
                        for (Person p : d.getGroup().getMembers()) {
                            Feedback.Status status = d.getStatusFor(p);
                            if (status.equals(Feedback.Status.COMMITTED)) {
                                summary.append("- ").append(p.getName()).append("\n");
                            } else if (status.equals(Feedback.Status.CANCELLED)) {
                                cancelled.append("- ").append(p.getName()).append("\n");
                            } else {
                                noAnswer.append("- ").append(p.getName()).append("\n");
                            }
                        }
                        if (d.getGroup().getMembers().stream().map(d::getStatusFor).noneMatch(Feedback.Status.COMMITTED::equals)) {
                            summary.append("--> Niemand\n");
                        }
                        if (d.getGroup().getMembers().stream().map(d::getStatusFor).noneMatch(Feedback.Status.CANCELLED::equals)) {
                            cancelled.append("--> Niemand\n");
                        }
                        if (d.getGroup().getMembers().stream().map(d::getStatusFor).noneMatch(Feedback.Status.DONTKNOW::equals)) {
                            noAnswer.append("--> Niemand\n");
                        }
                        summary.append("\n").append(cancelled).append("\n").append(noAnswer);
                        d.getGroup().getOrganisation().sendMessageTo(summary.toString(), admin);
                    });
                }

                for (Person p : d.getGroup().getMembers()) {
                    Feedback.Status feedback = d.getStatusFor(p);
                    if (!feedback.equals(Feedback.Status.CANCELLED)) {
                        if ((p.getRemindMeTime().contains(now.getHour()) && p.getDayReminderIntervals().contains((int) now.until(d.getStart(), ChronoUnit.DAYS)))
                                || p.getHourReminderIntervals().contains((int) now.until(d.getStart(), ChronoUnit.HOURS))) {
                            d.getGroup().getOrganisation().sendMessageTo(new MessageFormatter().person(p).date(d).format(DATE_REMINDER_TEMPLATE), p);
                        }
                    }
                }

            }
            for (Date d : datesService.getDateRepository().findByPollScheduledFor(LocalDate.now())) {
                LOGGER.info("Umfragen für {} am {} werden verschicken wird geprüft", d.getTitle(), d.getStart().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")));
                if (d.getPollScheduledFor() != null) {
                    if (LocalDate.now().isEqual(d.getPollScheduledFor())) {
                        if (d.isPollRunning()) {
                            for (Person p : d.getGroup().getMembers()) {
                                if (p.getRemindMeTime().contains(now.getHour())) {
                                    LOGGER.info("Umfragen für {} am {} wird an {} verschickt", d.getTitle(), d.getStart().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")), p.getName());
                                    try {
                                        d.getGroup().getOrganisation().sendDatePoll(d, p);
                                    } catch (FriendlyError e) {
                                        LOGGER.error("Die geplante Terminabfrage konnte nicht verschickt werden", e);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Throwable e) {
            LOGGER.error("Es ist ein Fehler für die Terminerinnerungen aufgetreten", e);
        }
        LOGGER.info("ERINNERUNGEN VERSCHICKEN {}: ENDE", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")));
        LOGGER.info("------------------------------------------------------------------------------------------------");
    }

    private static final String DATE_REMINDER_TEMPLATE =
            """
                    Hey #PERSON_FIRSTNAME,
                    du hast #DATE_TIME_UNTIL_START einen Termin bei der #ORGANISATION_NAME :)
                    #DATE_TITLE (#BOARD_TITLE)
                    Von #DATE_START_TIME am #DATE_START_DATE
                    Bis #DATE_END_TIME am #DATE_END_DATE
                                       \s
                    Deine Rückmeldung zu diesem Termin: #FEEDBACK_STATUS
                    Weitere Informationen zu diesem Termin findest du unter: #DATE_LINK""";
}