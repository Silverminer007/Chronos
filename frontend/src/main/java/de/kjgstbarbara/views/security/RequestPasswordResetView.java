package de.kjgstbarbara.views.security;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import de.kjgstbarbara.components.Header;
import de.kjgstbarbara.data.Person;
import de.kjgstbarbara.messaging.MessageSender;
import de.kjgstbarbara.messaging.Messages;
import de.kjgstbarbara.messaging.Platform;
import de.kjgstbarbara.service.PersonsRepository;
import de.kjgstbarbara.service.PersonsService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Random;

@Route("reset-password")
@AnonymousAllowed
public class RequestPasswordResetView extends VerticalLayout {
    private static final Logger LOGGER = LogManager.getLogger();
    private final PersonsRepository personsRepository;
    private final PasswordEncoder passwordEncoder;
    private Integer otp = null;
    private int attempts = 0;

    private Component header = new Div();
    private Component content = new Div();

    private Person person;


    public RequestPasswordResetView(PersonsService personsService, PasswordEncoder passwordEncoder) {
        this.personsRepository = personsService.getPersonsRepository();
        this.passwordEncoder = passwordEncoder;
        this.setWidthFull();
        this.setPadding(false);
        this.setSpacing(false);

        this.add(this.header);
        this.add(this.content);
        this.createHeader();
        this.createContent();
    }

    private void createHeader() {
        HorizontalLayout header = new Header();
        header.add(new H4("Passwort zurücksetzen"));

        Icon help = VaadinIcon.QUESTION_CIRCLE_O.create();
        help.setTooltipText("""
                Falls du dein Passwort oder deinen Benutzernamen vergessen hast,
                kannst du ihn über dieses Formular zurücksetzen.
                Dazu müssen wir deine Identität bestätigen, um sicher zu gehen,
                dass niemand diese Möglichkeit ausnutzt um Zugang zu deinem Account zu erhalten.
                Daher musst du hier die E-Mail Adresse angeben, mit der du deinen Account erstellt hast.
                Du erhältst dann eine E-Mail mit einem Einmalpasswort was du hier eingibst.
                Anschließend kannst du dein Passwort ändern""");
        header.add(help);

        this.replace(this.header, header);
        this.header = header;
    }

    private void createContent() {
        VerticalLayout content = new VerticalLayout();
        content.setWidthFull();

        TextField email = new TextField("E-Mail Adresse");
        IntegerField enterOTP = new IntegerField("Einmalpasswort");
        if (otp == null) {
            content.add(email);
            email.focus();
        } else {
            content.add(new NativeLabel("Wenn zu " + email.getValue() + " ein Account existiert, wurde an diesen ein Einmalpasswort verschickt. Bitte sieh in deinem Postfach nach ob du ein Einmalpasswort erhalten hast und gib dieses hier ein"));
            content.add(enterOTP);
            enterOTP.focus();
        }

        Button ok = new Button("Abschicken");
        ok.addClickShortcut(Key.ENTER);
        ok.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        ok.addClickListener(event -> {
            if (otp != null) {
                if (otp.equals(enterOTP.getValue()) && otp > 0) {
                    this.createEnterNewPassword(email.getValue());
                } else {
                    if(this.attempts++ > 4) {
                        Notification.show("Zu viele falsche Eingaben");
                        UI.getCurrent().getPage().getHistory().go(0);
                        this.otp = null;
                    }
                    enterOTP.setInvalid(true);
                    enterOTP.setErrorMessage("Das Einmalpasswort ist ungültig");
                    LOGGER.warn("Für [{}] wurde ein falsches Einmalpasswort beim Passwort zurücksetzen eingegeben", email.getValue());
                }
            } else {
                Optional<Person> optionalPerson = personsRepository.findByeMailAddress(email.getValue());
                if (optionalPerson.isPresent()) {
                    this.person = optionalPerson.get();
                    this.otp = new Random().nextInt(1_000_000);
                    new MessageSender(this.person).person(this.person).send(Messages.PERSON_RESET_PASSWORD.replaceAll("#PERSON_OTP", otp.toString()), Platform.EMAIL);
                } else {
                    this.otp = -1;
                    LOGGER.warn("Ungültiger Versuch das Passwort zurückzusetzen von [{}]", email.getValue());
                }
                this.createContent();
            }
        });
        content.add(ok);

        this.replace(this.content, content);
        this.content = content;
    }

    private void createEnterNewPassword(String emailAddress) {
        VerticalLayout content = new VerticalLayout();
        content.setWidthFull();

        content.add(new NativeLabel("Bitte gib ein starkes neues Passwort (12+ Zeichen empfohlen) ein und melde dich anschließend damit an"));

        PasswordField password = new PasswordField("Passwort");
        password.focus();
        content.add(password);

        PasswordField reTypePassword = new PasswordField("Passwort wiederholen");
        content.add(reTypePassword);

        Button ok = new Button("Passwort ändern");
        ok.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        ok.addClickShortcut(Key.ENTER);
        ok.addClickListener(event -> {
            if (!password.getValue().equals(reTypePassword.getValue())) {
                reTypePassword.setInvalid(true);
                reTypePassword.setErrorMessage("Die beiden Passwörter stimmen nicht überein");
                return;
            }
            if (password.getValue().length() < 6) {
                reTypePassword.setInvalid(true);
                reTypePassword.setErrorMessage("Das Passwort sollte mindestens 6 Zeichen lang sein");
                return;
            }
            this.person.setPassword(passwordEncoder.encode(password.getValue()));
            Notification.show("Das Passwort wurde erfolgreich zurückgesetzt");
            LOGGER.warn("Das Passwort von [{}] wurde zurückgesetzt", emailAddress);
            UI.getCurrent().navigate(LoginView.class);
        });
        content.add(ok);

        this.replace(this.content, content);
        this.content = content;
    }
}