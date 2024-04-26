package de.kjgstbarbara.messaging;

import de.kjgstbarbara.FriendlyError;
import de.kjgstbarbara.data.Person;
import it.auties.whatsapp.api.PairingCodeHandler;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.chat.Chat;
import it.auties.whatsapp.model.jid.Jid;

import java.util.Optional;

public class WhatsAppMessageSender implements IMessageSender {
    private final transient Whatsapp whatsapp;

    public WhatsAppMessageSender(long phoneNumber) {
        whatsapp = Whatsapp.webBuilder()
                .lastConnection()
                //.unregistered(QrHandler.toTerminal())
                .unregistered(phoneNumber, PairingCodeHandler.toTerminal())
                .addLoggedInListener(api -> {
                    System.out.printf("Connected: %s%n", api.store().privacySettings());
                    api.store().findChatByJid(Jid.of(4915752657194L)).ifPresent(chat -> {// TODO Nachricht ändern und einen Admin hinterlegen für dieses Update
                        api.sendMessage(chat, "Der WhatsApp Dienst wurde erfolgreich gestartet");
                    });
                })
                .addDisconnectedListener(reason -> System.out.printf("Disconnected: %s%n", reason))
                .addNewChatMessageListener(message -> System.out.printf("New message: %s%n", message.toJson()))
                .connect()
                .join();
        System.out.println("WhatsApp init");
    }

    @Override
    public void sendMessage(String message, Person sendTo) throws FriendlyError {
        Optional<Chat> optionalChat = whatsapp.store().findChatByJid(Jid.of(sendTo.getPhoneNumber()));
        if (optionalChat.isPresent()) {
            whatsapp.sendMessage(optionalChat.get(), message);
        } else {
            throw new FriendlyError("Es konnte kein Kontakt zur Telefonnummer +" + sendTo.getPhoneNumber() + " gefunden werden");
        }
    }
}