package de.kjgstbarbara.messaging;

import de.kjgstbarbara.FriendlyError;
import de.kjgstbarbara.data.*;
import de.kjgstbarbara.service.*;
import it.auties.whatsapp.model.info.MessageInfo;
import it.auties.whatsapp.model.message.standard.PollUpdateMessage;
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
    @Autowired
    private OrganisationService organisationService;
    @Autowired
    private FeedbackService feedbackService;

    @Override
    public void run(String... args) {
        LOGGER.info("Hourly runner setup");

        final ScheduledFuture<?> repeatingTask =
                SCHEDULER.scheduleAtFixedRate(this::runTasks, 0, 1, HOURS);
        SCHEDULER.schedule(() -> {
            repeatingTask.cancel(true);
        }, 60 * 60, SECONDS);

        /*organisationService.getOrganisationRepository().findAll().forEach(org -> {
            Whatsapp whatsapp = org.getWhatsapp();
            whatsapp.addNewChatMessageListener(this::newWAMessageListener);
            if (whatsapp.store().chats() == null || whatsapp.store().chats() != null && whatsapp.store().chats().isEmpty()) {
                whatsapp.disconnect();
            }
        });*/
    }

    /**
     * Runs every 60 Minutes
     */
    private void runTasks() {
        LOGGER.info("Hourly Updates run started");
        try {
            LocalDateTime now = LocalDateTime.now();
            LOGGER.info("Running hourly update checker for {} dates", datesService.getDateRepository().count());
            // Terminerinnerung → Im Profil Erinnerungen erstellen (in welchen Abständen) → Standard 1 Tag vorher, immer 19 Uhr gesammelt
            for (Date d : datesService.getDateRepository().findAll()) {
                LOGGER.info("Hourly update messages for date {}", d.getTitle());
                if (d.getStart().isBefore(now)) {
                    continue;
                }

                if (now.until(d.getStart(), ChronoUnit.HOURS) == 2) {
                    d.getGroup().getAdmins().forEach(admin -> {
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
                        summary.append(cancelled).append(noAnswer);
                        d.getGroup().getOrganisation().sendMessageTo(summary.toString(), admin);
                    });
                }

                for (Person p : d.getGroup().getMembers()) {
                    LOGGER.info("Hourly update messages for date {} and person {}", d.getTitle(), p.getName());
                    Feedback.Status feedback = d.getStatusFor(p);
                    LOGGER.info("Status der Rückmeldung: {}", feedback);
                    if (!feedback.equals(Feedback.Status.CANCELLED)) {
                        if ((p.getRemindMeTime().contains(now.getHour()) && p.getDayReminderIntervals().contains((int) now.until(d.getStart(), ChronoUnit.DAYS)))
                                || p.getHourReminderIntervals().contains((int) now.until(d.getStart(), ChronoUnit.HOURS))) {
                            d.getGroup().getOrganisation().sendMessageTo(new MessageFormatter().person(p).date(d).format(DATE_REMINDER_TEMPLATE), p);
                        }
                    }

                    LOGGER.info("Poll scheduled for {}, is running {}", d.getPollScheduledFor(), d.isPollRunning());
                    if (d.getPollScheduledFor() != null) {
                        LOGGER.info("Poll scheduled != null");
                        if(LocalDate.now().isEqual(d.getPollScheduledFor())) {
                            LOGGER.info("Poll scheduled for today");
                            if(d.isPollRunning()) {
                                LOGGER.info("Poll is running");
                                LOGGER.info("Remind me time [{}] and hour {}", p.getRemindMeTime(), now.getHour());
                                if(p.getRemindMeTime().contains(now.getHour())) {
                                    LOGGER.info("Hour suits");
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
        // Neue Termine?
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

    private void newWAMessageListener(MessageInfo newMessage) {
        newWAMessageListener(newMessage, datesService.getDateRepository(), personsService.getPersonsRepository(), feedbackService.getFeedbackRepository());
    }

    public static void newWAMessageListener(MessageInfo newMessage, DateRepository dateRepository, PersonsRepository personsRepository, FeedbackRepository feedbackRepository) {
        if (newMessage.message().content() instanceof PollUpdateMessage pollUpdateMessage) {
            try {
                if (pollUpdateMessage.votes() == null || pollUpdateMessage.votes().isEmpty()) {
                    return;
                }
                String selectedOption = pollUpdateMessage.votes().getFirst().name();
                if (!selectedOption.contains(")")) {
                    return;
                }
                selectedOption = selectedOption.substring(1, selectedOption.indexOf(')'));
                if (!selectedOption.contains("-")) {
                    return;
                }
                String dateIDString = selectedOption.substring(0, selectedOption.indexOf("-"));
                selectedOption = selectedOption.substring(selectedOption.indexOf("-"));
                int optionID = Integer.parseInt(selectedOption);
                long dateID = Long.parseLong(dateIDString);
                dateRepository.findById(dateID).ifPresent(date -> {
                    boolean pollRunning = date.isPollRunning() && LocalDateTime.now().isBefore(date.getStart());
                    StringBuilder phoneNumber = new StringBuilder(newMessage.senderJid().toPhoneNumber().substring(1));
                    phoneNumber.insert(2, " ");
                    phoneNumber.insert(6, " ");
                    personsRepository.findByPhoneNumber(new Person.PhoneNumber(phoneNumber.toString())).ifPresent(person -> {
                        if (pollUpdateMessage.votes().isEmpty()) {
                            return;
                        }
                        if (optionID > 0 && optionID < 3) {
                            if (pollRunning) {
                                Feedback feedback = Feedback.create(person, optionID == 1 ? Feedback.Status.COMMITTED : Feedback.Status.CANCELLED);
                                feedbackRepository.save(feedback);
                                date.addFeedback(feedback);
                                dateRepository.save(date);
                            } else {
                                date.getGroup().getOrganisation().sendMessageTo("Du kannst für diesen Termin nicht mehr abstimmen. Die Abstimmung ist bereits beendet worden. Alle Änderungen musst du jetzt persönlich mitteilen", person);
                            }
                        }
                    });
                });
            } catch (NumberFormatException ignored) {
            }
        }
    }
}
