package de.kjgstbarbara;

import de.kjgstbarbara.data.PasswordReset;
import de.kjgstbarbara.data.Person;
import de.kjgstbarbara.security.SecurityUtils;
import it.auties.whatsapp.api.PairingCodeHandler;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.chat.Chat;
import it.auties.whatsapp.model.jid.Jid;
import lombok.Getter;

import java.security.NoSuchAlgorithmException;
import java.util.Optional;

public class WhatsAppUtils {
    private static final long PHONE_NUMBER = 4915752657194L;
    @Getter
    private static final WhatsAppUtils instance = new WhatsAppUtils();
    private final transient Whatsapp whatsapp;

    private WhatsAppUtils() {
        whatsapp = Whatsapp.webBuilder()
                .lastConnection()
                //.unregistered(QrHandler.toTerminal())
                .unregistered(PHONE_NUMBER, PairingCodeHandler.toTerminal())
                .addLoggedInListener(api -> System.out.printf("Connected: %s%n", api.store().privacySettings()))
                .addDisconnectedListener(reason -> System.out.printf("Disconnected: %s%n", reason))
                .addNewChatMessageListener(message -> System.out.printf("New message: %s%n", message.toJson()))
                .connect()
                .join();
        System.out.println("WhatsApp init");
    }

    static void init() {
    }

    public PasswordReset sendPasswordResetMessage(Person requester) throws FriendlyError {
        PasswordReset passwordReset = new PasswordReset();
        passwordReset.setRequester(requester);
        String token;
        try {
            token = SecurityUtils.generatePassword(200);
        } catch (NoSuchAlgorithmException e) {
            throw new FriendlyError(e.getMessage());
        }
        passwordReset.setToken(token);
        String message = String.format("Hey %s %s,%ndu hast angefragt dein Passwort zurückzusetzen." +
                "%nWenn du das nicht getan hast klicke auf keinen Fall auf den Link unten, " +
                "sondern lösche diese Nachricht." +
                "%nUm dein Passwort zurückzusetzen, klicke auf diesen Link:" +
                "%nhttps://%s/password-reset/%s", requester.getFirstName(), requester.getLastName(), "localhost:8080", token);
        System.out.println(message);
        Optional<Chat> optionalChat = whatsapp.store().findChatByJid(Jid.of(requester.getPhoneNumber()));
        if (optionalChat.isPresent()) {
            whatsapp.sendMessage(optionalChat.get(), message);
        } else {
            //throw new FriendlyError("Es konnte kein Kontakt zur Telefonnummer +" + requester.getPhoneNumber() + " gefunden werden");
        }
        return passwordReset;
    }

}
