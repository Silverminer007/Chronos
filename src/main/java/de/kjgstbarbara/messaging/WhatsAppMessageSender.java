package de.kjgstbarbara.messaging;

import de.kjgstbarbara.FriendlyError;
import de.kjgstbarbara.data.Config;
import de.kjgstbarbara.data.Date;
import de.kjgstbarbara.data.Feedback;
import de.kjgstbarbara.data.Person;
import de.kjgstbarbara.service.ConfigService;
import de.kjgstbarbara.service.DatesService;
import de.kjgstbarbara.service.FeedbackService;
import de.kjgstbarbara.service.PersonsService;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.chat.Chat;
import it.auties.whatsapp.model.info.MessageInfo;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.message.model.Message;
import it.auties.whatsapp.model.message.standard.PollCreationMessage;
import it.auties.whatsapp.model.message.standard.PollCreationMessageBuilder;
import it.auties.whatsapp.model.message.standard.PollUpdateMessage;
import it.auties.whatsapp.model.poll.PollOption;
import it.auties.whatsapp.model.poll.PollOptionBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

@Component
public class WhatsAppMessageSender {
    private static final Logger LOGGER = LoggerFactory.getLogger(WhatsAppMessageSender.class);
    private final List<Consumer<String>> pairingCodeHandler = new ArrayList<>();
    private Whatsapp whatsapp;

    @Autowired
    private DatesService datesService;
    @Autowired
    private FeedbackService feedbackService;
    @Autowired
    private PersonsService personsService;
    @Autowired
    private ConfigService configService;

    public void setup() {
        whatsapp = Whatsapp.webBuilder()
                .lastConnection()
                .unregistered(configService.getLong(Config.Key.SENDER_PHONE_NUMBER),
                        authCode -> pairingCodeHandler.forEach(handler -> handler.accept(authCode)))
                .addLoggedInListener(api -> {
                    System.out.printf("Connected: %s%n", api.store().privacySettings());
                    for (Person admin : personsService.getPersonsRepository().systemAdmin()) {
                        api.store().findChatByJid(Jid.of(admin.getPhoneNumber())).ifPresent(chat ->
                                api.sendMessage(chat, "Der WhatsApp Dienst wurde erfolgreich gestartet"));
                    }
                })
                .addDisconnectedListener(reason -> LOGGER.info("Disconnected: {}", reason))
                .addNewChatMessageListener(message -> LOGGER.info("New message: {}}", message.toJson()))
                .addNewChatMessageListener(this::pollHandler)
                .connect()
                .join();
    }

    public void addCallback(Consumer<String> callback) {
        pairingCodeHandler.add(callback);
    }

    private Chat getChat(Person sendTo) throws FriendlyError {
        if (whatsapp == null) {
            setup();
        }
        Optional<Chat> optionalChat = whatsapp.store().findChatByJid(Jid.of(sendTo.getPhoneNumber()));
        if (optionalChat.isPresent()) {
            return optionalChat.get();
        } else {
            throw new FriendlyError("Es konnte kein Kontakt zur Telefonnummer +" + sendTo.getPhoneNumber() + " gefunden werden");
        }
    }

    public void sendMessage(String message, Person sendTo, boolean force) throws FriendlyError {
        if (sendTo.isWhatsappNotifications() || force) {
            whatsapp.sendMessage(getChat(sendTo), message);
        }
    }

    public void sendDatePoll(Date date, Person sendTo) throws FriendlyError {
        if (sendTo.isWhatsappNotifications()) {
            String title = String.format("(%s) Am %s um %s:%s Uhr ist %s. Bist du dabei?",
                    date.getId(),
                    date.getStart().getDayOfWeek().getDisplayName(TextStyle.FULL, sendTo.getUserLocale()),
                    date.getStart().getHour(),
                    date.getStart().getMinute(),
                    date.getTitle());
            Message message = new PollCreationMessageBuilder()
                    .title(title)
                    .selectableOptions(getDatePollOptions())
                    .selectableOptionsCount(1)
                    .build();
            whatsapp.sendMessage(getChat(sendTo), message);
        }
    }

    private List<PollOption> getDatePollOptions() {
        return List.of(
                new PollOptionBuilder().name("(1) Bin dabei").build(),
                new PollOptionBuilder().name("(2) Bin raus").build()
        );
    }

    private void pollHandler(MessageInfo messageInfo) {
        try {
            PollUpdateMessage pollUpdateMessage = messageInfo.message().pollUpdateMessage().orElse(null);
            if (pollUpdateMessage == null) return;
            if (pollUpdateMessage.votes().isEmpty()) return;

            PollCreationMessage pollCreationMessage = pollUpdateMessage.pollCreationMessage().orElse(null);
            if (pollCreationMessage == null) return;

            Long phoneNumber = pollUpdateMessage.voter().map(jid -> Long.parseLong(jid.toPhoneNumber().substring(1))).orElse(null);
            if (phoneNumber == null) return;

            Person person = personsService.getPersonsRepository().findByPhoneNumber(phoneNumber).orElse(null);
            if (person == null) return;

            String title = pollCreationMessage.title();
            long dateID = Long.parseLong(title.substring(1, title.indexOf(")")));
            Date date = datesService.getDateRepository().findById(dateID).orElse(null);
            if (date == null) return;

            String vote = pollUpdateMessage.votes().getFirst().name();
            long voteId = Long.parseLong(vote.substring(1, vote.indexOf(")")));

            if (voteId == 1 || voteId == 2) {
                Feedback feedback = Feedback.create(person, voteId == 1 ? Feedback.Status.IN : Feedback.Status.OUT);
                feedbackService.getFeedbackRepository().save(feedback);
                date.addFeedback(feedback);
                datesService.getDateRepository().save(date);
                System.out.printf("%s ist bei %s %s", person.getName(), date.getTitle(), feedback.getStatus());
            }
        } catch (NumberFormatException ignored) {
        }
    }
}