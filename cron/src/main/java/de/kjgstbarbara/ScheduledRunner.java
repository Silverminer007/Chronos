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
        LOGGER.info("ERINNERUNGEN VERSCHICKEN {}: START", this.formatDate(now));
        try {
            // Terminerinnerung → Im Profil Erinnerungen erstellen (in welchen Abständen) → Standard 1 Tag vorher, immer 19 Uhr gesammelt
            for (Date d : datesService.getDateRepository().findByStartBetween(now, now.plusDays(8)).stream().sorted(Comparator.comparing(Date::getStart)).toList()) {
                this.processNotifications(d);
            }
            for (Date d : datesService.getDateRepository().findByPollScheduledFor(LocalDate.now())) {
                LOGGER.info("Umfragen für {} am {} werden verschicken wird geprüft", d.getTitle(), this.formatDate(d.getStart()));
                if (d.getPollScheduledFor() == null) {
                    continue;
                }
                if (!LocalDate.now().isEqual(d.getPollScheduledFor())) {
                    continue;
                }
                if (!d.isPollRunning()) {
                    continue;
                }
                sendPoll(d);
            }
        } catch (Throwable e) {
            LOGGER.error("Es ist ein Fehler für die Terminerinnerungen aufgetreten", e);
        }
        LOGGER.info("ERINNERUNGEN VERSCHICKEN {}: ENDE", this.formatDate(LocalDateTime.now()));
        LOGGER.info("------------------------------------------------------------------------------------------------");
    }

    private void processNotifications(Date d) {
        LocalDateTime now = LocalDateTime.now();
        LOGGER.info("Erinnerungen für {} am {} werden verschickt", d.getTitle(), this.formatDate(d.getStart()));

        if (LocalDateTime.now().until(d.getStart(), ChronoUnit.HOURS) == 2) {
            this.processAdminOverviewMessage(d);
        }

        for (Person p : d.getGroup().getMembers()) {
            for (Person.Notification notification : p.getNotifications()) {
                if (now.until(d.getStart(), ChronoUnit.HOURS) == notification.getHoursBefore()) {
                    LOGGER.info("Erinnerungen für {} am {} werden an \"{}\" verschickt", d.getTitle(), this.formatDate(d.getStart()), p.getName());
                    new MessageSender(p).person(p).date(d).send(Messages.DATE_REMINDER, notification.getPlatform());
                }
            }
        }
    }

    private void processAdminOverviewMessage(Date d) {
        for (Person admin : d.getGroup().getMembers()) {
            new MessageSender(admin).send(this.buildAdminMessage(admin, d));
        }
    }

    private String buildAdminMessage(Person admin, Date d) {
        LOGGER.info("Termin Infos für {} am {} werden an {} verschickt", d.getTitle(), d.getStart(), admin.getName());
        StringBuilder summary = new StringBuilder();
        summary.append("Hey ").append(admin.getFirstName()).append(",\n");
        summary.append("gleich ist ").append(d.getTitle()).append("\n\n");
        summary.append("Dabei sind:\n");
        StringBuilder cancelled = new StringBuilder("Nicht dabei sind:\n");
        StringBuilder noAnswer = new StringBuilder("Bisher nicht gemeldet haben sich:\n");
        for (Person p : d.getGroup().getMembers()) {
            Feedback.Status status = d.getStatusFor(p);
            StringBuilder selection = switch (status) {
                case CANCELLED -> cancelled;
                case COMMITTED -> summary;
                default -> noAnswer;
            };
            selection.append(p.getName()).append("\n");
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
        return summary.toString();
    }

    private void sendPoll(Date d) {
        for (Person p : d.getGroup().getMembers()) {
            if (LocalDateTime.now().getHour() == 19) {// TODO Wo kommt diese Einstellung hin?
                LOGGER.info("Umfragen für {} am {} wird an {} verschickt", d.getTitle(), this.formatDate(d.getStart()), p.getName());
                new MessageSender(p).date(d).person(p).send(Messages.DATE_POLL);
            }
        }
    }

    private String formatDate(LocalDateTime date) {
        return date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
    }
}