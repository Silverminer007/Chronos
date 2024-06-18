package de.kjgstbarbara.data;

import de.kjgstbarbara.FriendlyError;
import de.kjgstbarbara.messaging.EMailSender;
import de.kjgstbarbara.messaging.MessageFormatter;
import de.kjgstbarbara.messaging.SignalSender;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.chat.Chat;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.message.model.Message;
import it.auties.whatsapp.model.message.standard.PollCreationMessageBuilder;
import it.auties.whatsapp.model.poll.PollOptionBuilder;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Semaphore;

@Entity
@Data
@NoArgsConstructor
public class Organisation {
    private static final Logger LOGGER = LoggerFactory.getLogger(Organisation.class);
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Id
    private long id;
    private String name;
    @ManyToOne
    private Person admin;
    @ManyToMany(fetch = FetchType.EAGER)
    private List<Person> members = new ArrayList<>();
    @ManyToMany(fetch = FetchType.EAGER)
    private List<Person> membershipRequests = new ArrayList<>();

    @Embedded
    private EMailSender emailSender = new EMailSender();
    @Embedded
    private SignalSender signalSender = new SignalSender();

    public String toString() {
        return name;
    }

    public UUID getIDAsUUID() {
        String hexId = Long.toHexString(id);
        StringBuilder uuidString = new StringBuilder();
        uuidString.append("0".repeat((32 - hexId.length())));
        uuidString.append(hexId);
        uuidString.insert(20, "-");
        uuidString.insert(16, "-");
        uuidString.insert(12, "-");
        uuidString.insert(8, "-");
        return UUID.fromString(uuidString.toString());
    }

    public Optional<Whatsapp> getWhatsapp() {
        try {
            Semaphore semaphore = new Semaphore(1);
            semaphore.acquire();
            Whatsapp whatsapp = Whatsapp.webBuilder().newConnection(getIDAsUUID()).unregistered(qr -> {
                LOGGER.error("WhatsApp ist not connected for: {}", name);
                semaphore.release();
            });
            try {
                whatsapp.connect().join();
                if (whatsapp.store().chats() != null && !whatsapp.store().chats().isEmpty()) {
                    semaphore.release();
                    return Optional.of(whatsapp);
                }
            } catch (Throwable throwable) {
                semaphore.release();
                LOGGER.error("WhatsApp Setup failed", throwable);
            }
            semaphore.acquire();
            whatsapp.disconnect();
            LOGGER.info("WhatsApp not available");
            return Optional.empty();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendMessageTo(String message, Person person) {
        sendMessageTo(message, person, person.getReminder());
    }

    public void sendMessageTo(String message, Person person, Person.Reminder reminder) {
        if (reminder.equals(Person.Reminder.SIGNAL)) {
            signalSender.sendMessage(message, person);
        } else if (getWhatsapp().isEmpty() || reminder.equals(Person.Reminder.EMAIL)) {
            emailSender.sendMessage(message, person);
        } else {
            LOGGER.info("Message send via WhatsApp");
            LOGGER.info(message);
            getWhatsapp().ifPresent(whatsapp -> getChat(person).ifPresent(chat ->
                    whatsapp.sendMessage(chat, message).join()));
            LOGGER.info("Sending successful");
        }
    }

    private Optional<Chat> getChat(Person sendTo) {
        Optional<Chat> optionalChat = getWhatsapp().flatMap(whatsapp -> whatsapp.store().findChatByJid(Jid.of(sendTo.getPhoneNumber().number())));
        if (optionalChat.isPresent()) {
            return optionalChat;
        } else {
            LOGGER.info("Es konnte kein Kontakt zur Telefonnummer +{} von {} gefunden werden", sendTo.getPhoneNumber().number(), sendTo.getName());
            return Optional.empty();
        }
    }

    public void sendDatePollToAll(Date date) throws FriendlyError {
        boolean error = false;
        for (Person p : date.getGroup().getMembers()) {
            try {
                sendDatePoll(date, p);
            } catch (FriendlyError e) {
                LOGGER.error("Fehler unterschlagen:", e);
                error = true;
            }
        }
        if (error) {
            throw new FriendlyError("Die Terminabfrage konnte nicht an alle Personen verschickt werden");
        }
    }

    public void sendDatePoll(Date date, Person person) throws FriendlyError {
        if (Feedback.Status.DONTKNOW.equals(date.getStatusFor(person))) {
            MessageFormatter messageFormatter = new MessageFormatter()
                    .person(person).date(date);
            sendMessageTo(messageFormatter.format(DATE_POLL_FORMAT), person);
            /*switch (person.getReminder()) {
                case WHATSAPP -> sendDatePollWhatsApp(date, person);
                case EMAIL -> send(date, person);
            }*/
        }
    }

    public void sendDatePollWhatsApp(Date date, Person sendTo) {
        getWhatsapp().ifPresent(api -> {
            String title = String.format("Am %s um %s:%s Uhr ist %s. Bist du dabei?",
                    date.getStart().getDayOfWeek().getDisplayName(TextStyle.FULL, sendTo.getUserLocale()),
                    date.getStart().getHour(),
                    date.getStart().getMinute(),
                    date.getTitle());
            Message message = new PollCreationMessageBuilder()
                    .title(title)
                    .selectableOptions(List.of(
                            new PollOptionBuilder().name("(" + date.getId() + "-1) Bin dabei").build(),
                            new PollOptionBuilder().name("(" + date.getId() + "-2) Bin raus").build()
                    ))
                    .selectableOptionsCount(1)
                    .build();
            System.out.println("Poll send");
            getChat(sendTo).ifPresent(chat -> {
                api.sendMessage(chat, message);
            });
        });
    }

    private static final String DATE_POLL_FORMAT =
            """
                    Hey #PERSON_FIRSTNAME,
                    am #DATE_START_DATE um #DATE_START_TIME ist #DATE_TITLE. Bist du dabei?
                    Wenn ja klicke bitte hier:
                    #DATE_LINK/vote/1/#PERSON_ID
                    Wenn nicht klicke bitte hier:
                    #DATE_LINK/vote/2/#PERSON_ID
                    """;
}