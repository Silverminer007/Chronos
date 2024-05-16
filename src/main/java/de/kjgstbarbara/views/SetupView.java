package de.kjgstbarbara.views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.spring.security.AuthenticationContext;
import de.kjgstbarbara.data.Config;
import de.kjgstbarbara.data.Person;
import de.kjgstbarbara.messaging.SenderUtils;
import de.kjgstbarbara.service.*;
import de.kjgstbarbara.views.components.PhoneNumberField;
import de.kjgstbarbara.views.nav.MainNavigationView;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.core.userdetails.UserDetails;

@Route(value = "setup", layout = MainNavigationView.class)
@RouteAlias("setup-initial")
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
            createStep1(senderUtils);
            createStep2(senderUtils);
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

    private void createStep1(SenderUtils senderUtils) {
        HorizontalLayout buttons = new HorizontalLayout();
        VerticalLayout pairingCodeSettings = getWhatsAppPairingcodeSettings(senderUtils);
        VerticalLayout qrCodeSettings = getWhatsAppQrCodeSettings(senderUtils);

        Button pairingCodeButton = new Button("Mit Telefonummer");
        Button qrCodeButton = new Button("Mit QR Code");
        pairingCodeButton.addClickListener(event -> {
            pairingCodeSettings.setVisible(true);
            qrCodeSettings.setVisible(false);
            qrCodeButton.setEnabled(true);
            pairingCodeButton.setEnabled(false);
        });
        qrCodeButton.addClickListener(event -> {
           pairingCodeSettings.setVisible(false);
           qrCodeSettings.setVisible(true);
           qrCodeButton.setEnabled(false);
           pairingCodeButton.setEnabled(true);
        });
        buttons.add(pairingCodeButton, qrCodeSettings);
        step1.add(buttons, pairingCodeSettings, qrCodeSettings);
    }

    private VerticalLayout getWhatsAppPairingcodeSettings(SenderUtils senderUtils) {
        VerticalLayout verticalLayout = new VerticalLayout();
        H1 whatsAppTitle = new H1("WhatsApp Einstellungen");
        verticalLayout.add(whatsAppTitle);

        PhoneNumberField phoneNumber = new PhoneNumberField();
        phoneNumber.setValue(new Person.PhoneNumber(configService.get(Config.Key.SENDER_PHONE_NUMBER)));

        verticalLayout.add(phoneNumber);
        H2 pairingCode = new H2("Kopplungscode kommt");
        pairingCode.setVisible(false);
        senderUtils.setWhatsAppPairingCodeHandler(code ->
                this.getUI().ifPresent(ui -> ui.access(() ->
                        pairingCode.setText("Kopplungscode: " + code))));
        verticalLayout.add(pairingCode);
        Button goOn = new Button("Weiter");
        verticalLayout.add(goOn);
        goOn.addClickListener(event -> {
            Person.Reminder reminder = this.person.getReminder();
            this.person.setReminder(Person.Reminder.WHATSAPP);
            personsRepository.save(this.person);

            configService.save(Config.Key.SENDER_PHONE_NUMBER, phoneNumber.getValue().toString());
            senderUtils.reSetupWhatsApp(false);

            boolean result = senderUtils.sendMessage("Test Nachricht", this.person);

            this.person.setReminder(reminder);
            personsRepository.save(this.person);

            if (result) {
                step1.setVisible(false);
                step2.setVisible(true);
                Notification.show("Die WhatsApp Konfiguration wurde erfolgreich gespeichert")
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } else if (pairingCode.isVisible()) {
                Notification.show("Koppeln nicht erfolgreich / noch nicht abgeschlossen")
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
            pairingCode.setVisible(true);
        });
        return verticalLayout;
    }

    private VerticalLayout getWhatsAppQrCodeSettings(SenderUtils senderUtils) {
        VerticalLayout verticalLayout = new VerticalLayout();
        Image qrCode = new Image();
        senderUtils.setWhatsAppQrCodeHandler(code ->
                this.getUI().ifPresent(ui -> ui.access(() ->
                        qrCode.setSrc(code))));
        return verticalLayout;
    }

    private void createStep2(SenderUtils senderUtils) {
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
            Person.Reminder reminder = this.person.getReminder();
            this.person.setReminder(Person.Reminder.EMAIL);
            personsRepository.save(this.person);

            configService.save(Config.Key.SENDER_NAME, name.getValue());
            configService.save(Config.Key.SMTP_PORT, smtpPort.getValue());
            configService.save(Config.Key.SENDER_EMAIL_ADDRESS, senderMailAddress.getValue());
            configService.save(Config.Key.SMTP_SERVER, smtpServer.getValue());
            configService.save(Config.Key.SMTP_PASSWORD, smtpPassword.getValue());

            boolean result = senderUtils.sendMessage("Test Nachricht", this.person);

            this.person.setReminder(reminder);
            personsRepository.save(this.person);

            if (result) {
                this.person.setSystemAdmin(true);
                personsRepository.save(person);
                configService.save(Config.Key.SETUP_DONE, true);
                UI.getCurrent().navigate("");
                Notification.show("Die E-Mail Konfiguration wurde erfolgreich gespeichert")
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } else {
                Notification.show("Die E-Mail Konfiguration ist fehlerhaft")
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
    }
}
