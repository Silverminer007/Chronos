package de.chronoslive.messaging;
import de.chronoslive.Result;
import de.chronoslive.entitys.Date;
import de.chronoslive.entitys.Feedback;
import de.chronoslive.entitys.Person;
import de.chronoslive.entitys.PollReminder;
import de.chronoslive.services.DatesService;
import de.chronoslive.repositorys.PollReminderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;

@Component
public class MessageScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageScheduler.class);

    private final DatesService datesService;
    private final PollReminderRepository pollReminderRepository;

    public MessageScheduler(DatesService datesService, PollReminderRepository pollReminderRepository) {
        this.datesService = datesService;
        this.pollReminderRepository = pollReminderRepository;
    }

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
            for (PollReminder pollReminder : pollReminderRepository.findByPollIntervalStartBeforeAndDateStartAfter(now, now)) {
                this.sendPollReminders(pollReminder);
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

        if (d.getDateCancelled() != null) {
            LOGGER.info("The Date was cancelled, no messages sent for this date");
            return;
        }

        if (LocalDateTime.now().until(d.getStart(), ChronoUnit.HOURS) == 2) {
            this.processAdminOverviewMessage(d);
        }

        for (Person p : d.getGroup().getMembers()) {
            if (!Feedback.Status.CANCELLED.equals(d.getStatusFor(p))) {
                continue;
            }
            for (Person.Notification notification : p.getNotifications()) {
                if (now.until(d.getStart(), ChronoUnit.HOURS) == notification.getHoursBefore()) {
                    LOGGER.info("Erinnerungen für {} am {} werden an \"{}\" verschickt", d.getTitle(), this.formatDate(d.getStart()), p.getName());
                    Result result = new MessageFormatter(p)
                            .person(p)
                            .date(d)
                            .organisation(d.getGroup().getOrganisation())
                            .group(d.getGroup())
                            .feedback(d.getStatusFor(p))
                            .send(Messages.DATE_REMINDER, notification.getPlatform());
                    if (result.isError()) {
                        LOGGER.error(result.getErrorMessage());
                    }
                }
            }
        }
    }

    private void processAdminOverviewMessage(Date d) {
        for (Person admin : d.getGroup().getAdmins()) {
            Result result = new MessageFormatter(admin)
                    .send(this.buildAdminMessage(admin, d));
            if (result.isError()) {
                LOGGER.error("Failed to send Admin Overview Message to {}, because {}", admin.getName(), result.getErrorMessage());
            }
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

    private String formatDate(LocalDateTime date) {
        return date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
    }

    public void sendPollReminders(PollReminder pollReminder) {
        if (pollReminder.getPollStarter() == null || pollReminder.getDate() == null) {
            return;
        }
        if (checkSendReminder(pollReminder, LocalDateTime.now())) {
            LOGGER.info("Abstimmungserinnerungen wurden für {} um {} verschickt", pollReminder.getDate().getTitle(), formatDate(LocalDateTime.now()));
            for (Person person : pollReminder.getDate().getGroup().getMembers()) {
                if (!pollReminder.getDate().getStatusFor(person).equals(Feedback.Status.NONE)) {
                    continue;
                }
                Result result = new MessageFormatter(person)
                        .person(person)
                        .person(pollReminder.getPollStarter(), "REQUESTER")
                        .date(pollReminder.getDate())
                        .send(Messages.DATE_POLL_REMINDER.replaceAll("#REMINDERS", "" + (pollReminder.getAmountOfTimesSend() + 1)));
                if (result.isError()) {
                    LOGGER.error(result.getErrorMessage());
                }
            }
            pollReminder.setAmountOfTimesSend(pollReminder.getAmountOfTimesSend() + 1);
            this.pollReminderRepository.save(pollReminder);
        }
    }

    public static boolean checkSendReminder(PollReminder pollReminder, LocalDateTime dateTime) {
        long pollReminderLength = pollReminder.getPollIntervalStart().until(pollReminder.getPollIntervalEnd(), ChronoUnit.HOURS);
        long passedReminderTime = pollReminder.getPollIntervalStart().until(dateTime, ChronoUnit.HOURS);

        if (pollReminder.getDate().getStart().isBefore(dateTime)) {
            return false;
        }

        if (passedReminderTime > pollReminderLength) {
            return true;
        }

        if (passedReminderTime < 0) {
            return false;
        }

        for (int i = 0; i < pollReminderLength; i++) {
            double step = Math.pow(2, i);
            long pollSendTime = (long) Math.ceil((pollReminderLength * (step - 1)) / step);
            if (passedReminderTime < pollSendTime) {
                break;
            }
            if (passedReminderTime > pollSendTime) {
                continue;
            }
            return true;
        }
        return false;
    }
}