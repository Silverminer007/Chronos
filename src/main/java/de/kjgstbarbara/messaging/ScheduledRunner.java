package de.kjgstbarbara.messaging;

import de.kjgstbarbara.FriendlyError;
import de.kjgstbarbara.data.*;
import de.kjgstbarbara.service.*;
import it.auties.whatsapp.api.Whatsapp;
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
                SCHEDULER.scheduleAtFixedRate(this::runTasks, 0, 1, MINUTES);
        SCHEDULER.schedule(() -> {
            repeatingTask.cancel(true);
        }, 60 * 60, SECONDS);

        organisationService.getOrganisationRepository().findAll().forEach(org -> {
            Whatsapp whatsapp = org.getWhatsapp();
            whatsapp.addNewChatMessageListener(this::newWAMessageListener);
            if (whatsapp.store().chats() == null || whatsapp.store().chats() != null && whatsapp.store().chats().isEmpty()) {
                whatsapp.disconnect();
            }
        });
    }

    /**
     * Runs every 60 Minutes
     */
    private void runTasks() {
        LocalDateTime now = LocalDateTime.now();
        // Terminerinnerung → Im Profil Erinnerungen erstellen (in welchen Abständen) → Standard 1 Tag vorher, immer 19 Uhr gesammelt
        for (Date d : datesService.getDateRepository().findAll()) {
            if(d.getStart().isBefore(now)) {
                continue;
            }
            for (Person p : d.getGroup().getMembers()) {
                Feedback.Status feedback = d.getStatusFor(p);
                if (!feedback.equals(Feedback.Status.CANCELLED)) {
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
                    du hast #DATE_TIME_UNTIL_START einen Termin bei der #ORGANISATION_NAME :)
                    #DATE_TITLE (#BOARD_TITLE)
                    Von #DATE_START_TIME am #DATE_START_DATE
                    Bis #DATE_END_TIME am #DATE_END_DATE
                                       \s
                    Deine Rückmeldung zu diesem Termin: #FEEDBACK_STATUS""";//Weitere Informationen zu diesem Termin findest du unter: #DATE_LINK

    private void newWAMessageListener(MessageInfo newMessage) {
        newWAMessageListener(newMessage, datesService.getDateRepository(), personsService.getPersonsRepository(), feedbackService.getFeedbackRepository());
    }

    public static void newWAMessageListener(MessageInfo newMessage, DateRepository dateRepository, PersonsRepository personsRepository, FeedbackRepository feedbackRepository) {
        LOGGER.info(newMessage.message().unbox().content().type().toString());
        if (newMessage.message().content() instanceof PollUpdateMessage pollUpdateMessage) {
            LOGGER.info(newMessage.message().content().toString());
            try {
                if(pollUpdateMessage.votes() == null || pollUpdateMessage.votes().isEmpty()) {
                    return;
                }
                String selectedOption = pollUpdateMessage.votes().getFirst().name();
                if (!selectedOption.contains(")")) {
                    return;
                }
                selectedOption = selectedOption.substring(1, selectedOption.indexOf(')'));
                if(!selectedOption.contains("-")) {
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
                                date.getGroup().getOrganisation().getWhatsapp().sendMessage(newMessage.senderJid(), "Du kannst für diesen Termin nicht mehr abstimmen. Die Abstimmung ist bereits beendet worden. Alle Änderungen musst du jetzt persönlich mitteilen");
                            }
                        }
                    });
                });
            } catch (NumberFormatException ignored) {
            }
        }
    }
}
