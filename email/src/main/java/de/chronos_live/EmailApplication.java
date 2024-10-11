package de.chronos_live;

import org.simplejavamail.api.email.Email;
import org.simplejavamail.api.mailer.Mailer;
import org.simplejavamail.api.mailer.config.TransportStrategy;
import org.simplejavamail.email.EmailBuilder;
import org.simplejavamail.mailer.MailerBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@SpringBootApplication
@RestController
public class EmailApplication {
    private static final String SMTP_SERVER = System.getenv("SMTP_SERVER");
    private static final String SMTP_USER = System.getenv("SMTP_USER");
    private static final String SMTP_MAIL_ADDRESS = System.getenv("SMTP_MAIL_ADDRESS");
    private static final String SMTP_PASSWORD = System.getenv("SMTP_PASSWORD");
    private static final int SMTP_PORT = Integer.parseInt(System.getenv("SMTP_PORT"));
    private static final boolean SMTP_SSL = Boolean.parseBoolean(System.getenv("SMTP_SSL"));

    public static void main(String[] args) {
        SpringApplication.run(EmailApplication.class, args);
    }

    @GetMapping(value = "/ping")
    public String ping() {
        return "200";
    }

    private boolean testMailSchema(String address) {
        return address.matches("[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}");
    }

    @PutMapping(value = "/send")
    public void sendReminder(@RequestBody Message message) {
        if (!testMailSchema(message.getTo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email Schema not valid");
        }
        if (message.getSubject().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Subject must not be blank");
        }
        Mailer mailer = MailerBuilder
                .withSMTPServer(SMTP_SERVER,
                        SMTP_PORT,
                        SMTP_MAIL_ADDRESS,
                        SMTP_PASSWORD)
                .withTransportStrategy(SMTP_SSL ?
                        TransportStrategy.SMTPS :
                        TransportStrategy.SMTP
                ).buildMailer();
        Email email = EmailBuilder.startingBlank()
                .from(SMTP_USER, SMTP_MAIL_ADDRESS)
                .to(message.getTo())
                .withSubject(message.getSubject())
                .withPlainText(message.getMessage())
                .buildEmail();
        mailer.sendMail(email);
    }
}
