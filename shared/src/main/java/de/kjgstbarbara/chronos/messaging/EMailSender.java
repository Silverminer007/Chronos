package de.kjgstbarbara.chronos.messaging;

import de.kjgstbarbara.chronos.Result;
import de.kjgstbarbara.chronos.data.Date;
import de.kjgstbarbara.chronos.data.Person;
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

    public Result sendMessage(String message, Person sendTo) {
        if(senderEmailAddress == null || senderName == null || smtpServer == null || smtpPassword == null) {
            return Result.error("No E-Mail Server is configured. Message failed to send");
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
        return Result.success();
    }

    public Result sendDatePoll(Date date, Person sendTo) {
        return sendMessage(date.getTitle(), sendTo);
    }
}