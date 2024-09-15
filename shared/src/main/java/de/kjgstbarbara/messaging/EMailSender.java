package de.kjgstbarbara.messaging;

import de.kjgstbarbara.FriendlyError;
import de.kjgstbarbara.data.Date;
import de.kjgstbarbara.data.Person;
import jakarta.annotation.Nullable;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.simplejavamail.api.email.Email;
import org.simplejavamail.api.mailer.Mailer;
import org.simplejavamail.api.mailer.config.TransportStrategy;
import org.simplejavamail.email.EmailBuilder;
import org.simplejavamail.mailer.MailerBuilder;

@Data
@NoArgsConstructor
public class EMailSender {
    @Nullable
    private String senderEmailAddress;
    @Nullable
    private String senderName;
    @Nullable
    private String smtpServer;
    private int smtpServerPort = -1;
    @Nullable
    private String smtpPassword;

    public void sendMessage(String message, Person sendTo) {
        if(senderEmailAddress == null || senderName == null || smtpServer == null || smtpPassword == null) {
            return;
        }
        Mailer mailer = MailerBuilder
                .withSMTPServer(smtpServer,
                        smtpServerPort,
                        senderEmailAddress,
                        smtpPassword)
                .withTransportStrategy(TransportStrategy.SMTPS).buildMailer();
        Email email = EmailBuilder.startingBlank()
                .from(senderName, senderEmailAddress)
                .to(sendTo.getName(), sendTo.getEMailAddress())
                .withSubject("Neue Nachricht vom KjG Termintool")// TODO Message
                .withPlainText(message)
                .buildEmail();
        mailer.sendMail(email);
    }

    public void sendDatePoll(Date date, Person sendTo) throws FriendlyError {
        sendMessage(date.getTitle(), sendTo);
    }
}