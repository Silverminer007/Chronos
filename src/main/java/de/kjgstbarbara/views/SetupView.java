package de.kjgstbarbara.views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;
import de.kjgstbarbara.data.Config;
import de.kjgstbarbara.data.Person;
import de.kjgstbarbara.messaging.SenderUtils;
import de.kjgstbarbara.service.*;
import de.kjgstbarbara.views.components.LongNumberField;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.core.userdetails.UserDetails;

@Route("setup")
@PermitAll
public class SetupView extends VerticalLayout implements BeforeEnterObserver {
    private final PersonsRepository personsRepository;
    private final ConfigService configService;

    private final Person person;

    private final VerticalLayout step1 = new VerticalLayout();
    private final VerticalLayout step2 = new VerticalLayout();

    public SetupView(PersonsService personsService, ConfigService configService, SenderUtils senderUtils, AuthenticationContext authenticationContext) {
        this.personsRepository = personsService.getPersonsRepository();
        this.configService = configService;
        this.person = authenticationContext.getAuthenticatedUser(UserDetails.class)
                .flatMap(userDetails -> personsRepository.findByUsername(userDetails.getUsername()))
                .orElse(null);
        if (person == null) {
            authenticationContext.logout();
        } else {
            step1.setVisible(true);
            step2.setVisible(false);

            // Step 1
            H1 whatsAppTitle = new H1("WhatsApp Einstellungen");
            step1.add(whatsAppTitle);
            LongNumberField senderPhoneNumber = new LongNumberField("Telefonnummer des Absenders");
            step1.add(senderPhoneNumber);
            H2 pairingCode = new H2();
            step1.add(pairingCode);
            pairingCode.setVisible(false);
            senderUtils.addWhatsAppPairingCodeHandler(code -> {
                pairingCode.setVisible(true);
                pairingCode.setText(code);
            });
            Button goOn = new Button("Weiter");
            step1.add(goOn);
            goOn.addClickListener(event -> {
                boolean mailNotifications = this.person.isEMailNotifications();
                boolean whatsAppNotifications = this.person.isWhatsappNotifications();
                this.person.setEMailNotifications(false);
                this.person.setWhatsappNotifications(true);
                personsRepository.save(this.person);

                configService.save(Config.Key.SENDER_PHONE_NUMBER, senderPhoneNumber.getValue());
                senderUtils.reSetupWhatsApp();

                boolean result = senderUtils.sendMessage("Test Nachricht", this.person, true);

                this.person.setEMailNotifications(mailNotifications);
                this.person.setWhatsappNotifications(whatsAppNotifications);
                personsRepository.save(this.person);

                if (result) {
                    step1.setVisible(false);
                    step2.setVisible(true);
                    Notification.show("Die WhatsApp Konfiguration wurde erfolgreich gespeichert")
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                } else if(pairingCode.isVisible()) {
                    Notification.show("Die Nachricht konnte nicht verschickt werden")
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            });


            // Step 2
            H1 eMailTitle = new H1("E-Mail Konfiguration");
            step2.add(eMailTitle);
            TextField smtpServer = new TextField("SMTP Server");
            step2.add(smtpServer);
            IntegerField smtpPort = new IntegerField("SMTP Port");
            step2.add(smtpPort);
            TextField name = new TextField("Name des Absenders");
            step2.add(name);
            TextField senderMailAddress = new TextField("E-Mail Adresse des Absenders");
            step2.add(senderMailAddress);
            PasswordField smtpPassword = new PasswordField("Password des Absenders");
            step2.add(smtpPassword);
            Button testMail = new Button("Speichern");
            step2.add(testMail);
            testMail.addClickListener(event -> {
                boolean mailNotifications = this.person.isEMailNotifications();
                boolean whatsAppNotifications = this.person.isWhatsappNotifications();
                this.person.setEMailNotifications(true);
                this.person.setWhatsappNotifications(false);
                personsRepository.save(this.person);

                configService.save(Config.Key.SENDER_NAME, name.getValue());
                configService.save(Config.Key.SMTP_PORT, smtpPort.getValue());
                configService.save(Config.Key.SENDER_EMAIL_ADDRESS, senderMailAddress.getValue());
                configService.save(Config.Key.SMTP_SERVER, smtpServer.getValue());
                configService.save(Config.Key.SMTP_PASSWORD, smtpPassword.getValue());

                boolean result = senderUtils.sendMessage("Test Nachricht", this.person, true);

                this.person.setEMailNotifications(mailNotifications);
                this.person.setWhatsappNotifications(whatsAppNotifications);
                personsRepository.save(this.person);

                if (result) {
                    configService.save(Config.Key.SETUP_DONE, true);
                    UI.getCurrent().navigate("");
                    Notification.show("Die E-Mail Konfiguration wurde erfolgreich gespeichert")
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                } else {
                    Notification.show("Die E-Mail Konfiguration ist fehlerhaft")
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            });
            this.add(step1, step2);
            // TODO Was ist wenn keine E-Mail Adresse hinterlegt ist?
            // TODO E-Mail Adresse und Telefon bestätigen lassen bei jeder Änderung
        }
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        if (!(this.person.isSystemAdmin() || !configService.getBoolean(Config.Key.SETUP_DONE))) {
            beforeEnterEvent.rerouteTo("");
        }
    }
}
