package de.kjgstbarbara.data;

import de.kjgstbarbara.FriendlyError;
import de.kjgstbarbara.messaging.EMailSender;
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

    public Whatsapp getWhatsapp() {
        try {
            Semaphore semaphore = new Semaphore(1);
            semaphore.acquire();
            Whatsapp whatsapp = Whatsapp.webBuilder().newConnection(getIDAsUUID()).unregistered(qr -> {
                LOGGER.error("WhatsApp ist not connected for: {}", name);
                semaphore.release();
            });
            try {
                whatsapp.addLoggedInListener(api -> {
                    semaphore.release();
                });
                whatsapp.connect().join();
                if (whatsapp.store().chats() != null && !whatsapp.store().chats().isEmpty()) {
                    semaphore.release();
                }
            } catch (Throwable throwable) {
                semaphore.release();
                LOGGER.error("WhatsApp Setup failed", throwable);
            }
            semaphore.acquire();
            return whatsapp;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendMessageTo(String message, Person person) throws FriendlyError {
        sendMessageTo(message, person, person.getReminder());
    }

    public void sendMessageTo(String message, Person person, Person.Reminder reminder) throws FriendlyError {
        if (getWhatsapp() == null || reminder.equals(Person.Reminder.EMAIL)) {
            emailSender.sendMessage(message, person);
        } else {
            getWhatsapp().sendMessage(getChat(person), message);
        }
    }

    private Chat getChat(Person sendTo) throws FriendlyError {
        Optional<Chat> optionalChat = getWhatsapp().store().findChatByJid(Jid.of(sendTo.getPhoneNumber().number()));
        if (optionalChat.isPresent()) {
            return optionalChat.get();
        } else {
            throw new FriendlyError("Es konnte kein Kontakt zur Telefonnummer +" + sendTo.getPhoneNumber().number() + " gefunden werden");
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
        if (date.getStatusFor(person) == Feedback.Status.DONTKNOW) {
            switch (person.getReminder()) {
                case WHATSAPP -> sendDatePollWhatsApp(date, person);
                case EMAIL -> sendDatePollEmail(date, person);
            }
        }
    }

    public void sendDatePollWhatsApp(Date date, Person sendTo) throws FriendlyError {
        Whatsapp api = getWhatsapp();
        String title = String.format("(%s) Am %s um %s:%s Uhr ist %s. Bist du dabei?",
                date.getId(),
                date.getStart().getDayOfWeek().getDisplayName(TextStyle.FULL, sendTo.getUserLocale()),
                date.getStart().getHour(),
                date.getStart().getMinute(),
                date.getTitle());
        Message message = new PollCreationMessageBuilder()
                .title(title)
                .selectableOptions(List.of(
                        new PollOptionBuilder().name("(1) Bin dabei").build(),
                        new PollOptionBuilder().name("(2) Bin raus").build()
                ))
                .selectableOptionsCount(1)
                .build();
        System.out.println("Poll send");
        api.sendMessage(getChat(sendTo), message);
    }

    public void sendDatePollEmail(Date date, Person person) {
        // TODO
    }
}