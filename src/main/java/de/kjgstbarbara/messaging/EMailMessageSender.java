package de.kjgstbarbara.messaging;

import de.kjgstbarbara.FriendlyError;
import de.kjgstbarbara.data.Config;
import de.kjgstbarbara.data.Date;
import de.kjgstbarbara.data.Person;
import de.kjgstbarbara.service.ConfigService;
import org.simplejavamail.api.email.Email;
import org.simplejavamail.api.mailer.Mailer;
import org.simplejavamail.api.mailer.config.TransportStrategy;
import org.simplejavamail.email.EmailBuilder;
import org.simplejavamail.mailer.MailerBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class EMailMessageSender {
    @Autowired
    private ConfigService configService;

    public void sendMessage(String message, Person sendTo) {
        Mailer mailer = MailerBuilder
                .withSMTPServer(configService.get(Config.Key.SMTP_SERVER),
                        configService.getInt(Config.Key.SMTP_PORT),
                        configService.get(Config.Key.SENDER_EMAIL_ADDRESS),
                        configService.get(Config.Key.SMTP_PASSWORD))
                .withTransportStrategy(TransportStrategy.SMTPS).buildMailer();
        Email email = EmailBuilder.startingBlank()
                .from(configService.get(Config.Key.SENDER_NAME), configService.get(Config.Key.SENDER_EMAIL_ADDRESS))
                .to(sendTo.getName(), sendTo.getEMailAddress())
                .withSubject("Neue Nachricht vom KjG Termintool")
                .withPlainText(message)
                .buildEmail();
        mailer.sendMail(email);
    }

    public void sendDatePoll(Date date, Person sendTo) throws FriendlyError {
        sendMessage(date.getTitle(), sendTo);
    }
}