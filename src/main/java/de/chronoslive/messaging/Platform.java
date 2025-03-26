package de.chronoslive.messaging;

import de.chronoslive.Result;
import de.chronoslive.entitys.Person;
import org.apache.logging.log4j.LogManager;
import org.simplejavamail.api.email.Email;
import org.simplejavamail.api.mailer.Mailer;
import org.simplejavamail.api.mailer.config.TransportStrategy;
import org.simplejavamail.email.EmailBuilder;
import org.simplejavamail.mailer.MailerBuilder;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.function.BiFunction;

public enum Platform {
    EMAIL((message, sendTo) -> {
        if(!sendTo.getEMailAddress().matches("[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}")) {
            return Result.error("Invalid e-mail address for User " + sendTo.getName());
        }
        String SMTP_SERVER = System.getenv("SMTP_SERVER");
        String SMTP_USER = System.getenv("SMTP_USER");
        String SMTP_MAIL_ADDRESS = System.getenv("SMTP_MAIL_ADDRESS");
        String SMTP_PASSWORD = System.getenv("SMTP_PASSWORD");
        String SMTP_PORT_STRING = System.getenv("SMTP_PORT");
        if(SMTP_PORT_STRING == null || SMTP_PORT_STRING.isBlank()) {
            return Result.error("Invalid SMTP_PORT, please specify a valid port number.");
        }
        int SMTP_PORT = Integer.parseInt(SMTP_PORT_STRING);
        try (Mailer mailer = MailerBuilder
                .withSMTPServer(SMTP_SERVER,
                        SMTP_PORT,
                        SMTP_MAIL_ADDRESS,
                        SMTP_PASSWORD)
                .withTransportStrategy(SMTP_PORT == 587 ?
                        TransportStrategy.SMTP_TLS :
                        (SMTP_PORT == 465 ?
                                TransportStrategy.SMTPS :
                                TransportStrategy.SMTP)
                ).buildMailer()) {
            Email email = EmailBuilder.startingBlank()
                    .from(SMTP_USER, SMTP_MAIL_ADDRESS)
                    .to(sendTo.getEMailAddress())
                    .withSubject("Chronos Message")
                    .withPlainText(message)
                    .buildEmail();
            mailer.sendMail(email, false);
        } catch (Exception e) {
            LogManager.getLogger(Platform.class).error(e);
            return Result.error("Die Nachricht konnte nicht an " + sendTo.getEMailAddress() + " verschickt werden");
        }
        return Result.success();
    }),
    SIGNAL((message, sendTo) -> {
        RestClient restClient = RestClient.create("http://signal:8080/v2/send");
        try {
            restClient.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new Signal(message, System.getenv("FROM_NUMBER"), new String[]{sendTo.getPhoneNumber().toString()}, "normal"))
                    .header("Content-Type", "application/json")
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve().toBodilessEntity();
            return Result.success();
        } catch (RestClientException e) {
            return Result.error("Die Nachricht konnte nicht an " + sendTo.getEMailAddress() + " verschickt werden");
        }
    });

    private final BiFunction<String, Person, Result> send;

    Platform(BiFunction<String, Person, Result> send) {
        this.send = send;
    }

    public Result send(Person person, String message) {
        return this.send.apply(message, person);
    }

    private record Signal(String message, String number, String[] recipients, String text_mode) {
    }
}
