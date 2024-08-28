package de.kjgstbarbara.chronos.data;

import de.kjgstbarbara.chronos.Result;
import de.kjgstbarbara.chronos.messaging.EMailSender;
import de.kjgstbarbara.chronos.messaging.MessageFormatter;
import de.kjgstbarbara.chronos.messaging.Messages;
import de.kjgstbarbara.chronos.messaging.SignalSender;
import de.kjgstbarbara.chronos.whatsapp.WhatsAppInstances;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.chat.Chat;
import it.auties.whatsapp.model.jid.Jid;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
    private List<Person> visible = new ArrayList<>();
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
        return Optional.ofNullable(WhatsAppInstances.getWhatsApp(this.id));
    }

    public Result sendMessageTo(String message, Person person) {
        return sendMessageTo(message, person, person.getReminder());
    }

    public Result sendMessageTo(String message, Person person, Person.Reminder reminder) {
        if (reminder.equals(Person.Reminder.SIGNAL)) {
            Result result = signalSender.sendMessage(message, person);
            if (result.isError()) {
                return Result.error(result.getErrorMessage() + ", falling back to WhatsApp").and(sendMessageTo(message, person, Person.Reminder.WHATSAPP));
            }
            return Result.success();
        } else if (reminder.equals(Person.Reminder.WHATSAPP)) {
            LOGGER.info("Message send via WhatsApp");
            LOGGER.info(message);

            Result result = getWhatsapp().map(whatsapp -> getChat(person).map(chat -> {
                whatsapp.sendMessage(chat, message).join();
                return Result.success();
            }).orElse(Result.error(person.getName() + " does not use WhatsApp, falling back to E-Mail"))).orElse(Result.error("WhatsApp is not available for " + this.getName() + ", falling back to E-Mail"));
            if (result.isError()) {
                return result.and(sendMessageTo(message, person, Person.Reminder.EMAIL));
            }
            LOGGER.info("Sending successful");
            return Result.success();
        } else {
            return emailSender.sendMessage(message, person);
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

    public Result sendDatePollToAll(Date date) {
        Result result = Result.success();
        for (Person p : date.getGroup().getMembers()) {
            result.and(sendDatePoll(date, p));
        }
        return result;
    }

    public Result sendDatePoll(Date date, Person person) {
        if (Date.Feedback.Status.NONE.equals(date.getStatusFor(person))) {
            MessageFormatter messageFormatter = new MessageFormatter()
                    .person(person).date(date);
            return sendMessageTo(messageFormatter.format(Messages.DATE_POLL), person);
        }
        return Result.error(person.getName() + " already gave feedback to " + date.getTitle());
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Organisation org && this.id == org.id;
    }

    @Override
    public int hashCode() {
        return (int) this.id;
    }
}