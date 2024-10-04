package de.kjgstbarbara;

import de.kjgstbarbara.data.Date;
import de.kjgstbarbara.data.Feedback;
import de.kjgstbarbara.data.Person;
import de.kjgstbarbara.messaging.MessageSender;
import de.kjgstbarbara.messaging.Messages;
import de.kjgstbarbara.service.DatesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;

@Component
public class ScheduledRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledRunner.class);

    @Autowired
    private DatesService datesService;

    @Scheduled(cron = "0 0 * * * *")
    public void run() {
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
                        if (d.getGroup().getMembers().stream().map(d::getStatusFor).noneMatch(Feedback.Status.NONE::equals)) {
                            noAnswer.append("--> Niemand\n");
                        }
                        summary.append("\n").append(cancelled).append("\n").append(noAnswer);
                        new MessageSender(admin).send(summary.toString());
                    });
                }

                for (Person p : d.getGroup().getMembers()) {
                    Feedback.Status feedback = d.getStatusFor(p);
                    if (!feedback.equals(Feedback.Status.CANCELLED)) {
                        if ((p.getRemindMeTime().contains(now.getHour()) && p.getDayReminderIntervals().contains((int) now.until(d.getStart(), ChronoUnit.DAYS)))
                                || p.getHourReminderIntervals().contains((int) now.until(d.getStart(), ChronoUnit.HOURS))) {
                            new MessageSender(p).person(p).date(d).send(Messages.DATE_REMINDER);
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
                                    new MessageSender(p).date(d).send(Messages.DATE_POLL);
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
}