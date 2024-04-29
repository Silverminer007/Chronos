package de.kjgstbarbara.views.security;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import de.kjgstbarbara.messaging.SenderUtils;
import de.kjgstbarbara.views.components.ReCaptcha;
import de.kjgstbarbara.data.Person;
import de.kjgstbarbara.service.PersonsRepository;
import de.kjgstbarbara.service.PersonsService;
import de.kjgstbarbara.views.components.LongNumberField;

import java.util.Optional;

@Route("request-password-reset")
@AnonymousAllowed
public class RequestPasswordResetView extends VerticalLayout {
    private static final String RESET_MESSAGE_TEMPLATE =
            """
            Hey #PERSON_NAME,
            du hast angefragt dein Passwort zurückzusetzen.
            Wenn du das nicht getan hast klicke auf keinen Fall auf den Link unten,
            sondern lösche diese Nachricht.
            
            Um dein Passwort zurückzusetzen, klicke auf diesen Link:
            #PERSON_RESET_LINK
            
            Der Link ist noch #PERSON_RESET_EXPIRES_IN Stunden gültig
            """;

    public RequestPasswordResetView(PersonsService personsService, SenderUtils senderUtils) {
        PersonsRepository personsRepository = personsService.getPersonsRepository();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        VerticalLayout wrapper = new VerticalLayout();
        wrapper.setWidthFull();
        wrapper.setHeightFull();
        wrapper.setAlignItems(Alignment.CENTER);
        wrapper.setJustifyContentMode(JustifyContentMode.CENTER);
        VerticalLayout inner = new VerticalLayout();
        wrapper.add(inner);
        inner.setWidth(200, Unit.MM);
        inner.add(new H1("Passwort zurücksetzen"));
        inner.add(new NativeLabel("Bitte gib deine Telefonnummer an," +
                " um dir einen Code zum Zurücksetzen deines Passworts zuschicken zu lassen" +
                " und gib dein Geburtsdatum als Bestätigung deiner Identität ein"));
        LongNumberField phoneNumber = new LongNumberField("Telefonnummer"); // TODO Vorwahl Auswahlfeld daneben
        phoneNumber.setWidthFull();
        inner.add(phoneNumber);
        ReCaptcha reCaptcha = new ReCaptcha();
        inner.add(reCaptcha);
        Button confirm = new Button("Bestätigen");
        confirm.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        confirm.addClickShortcut(Key.ENTER);
        confirm.addClickListener(event -> {
            if (reCaptcha.isValid()) {
                confirm.setAriaLabel("Bitte löse zuerst das Captcha");
            }
            if (phoneNumber.getValue() == null) {
                phoneNumber.setInvalid(true);
                phoneNumber.setErrorMessage("Bitte gib eine Telefonnummer an");
                return;
            }
            Optional<Person> optionalPerson = personsRepository.findByPhoneNumber(phoneNumber.getValue());
            if (optionalPerson.isPresent()) {
                phoneNumber.setInvalid(false);
                UI.getCurrent().getPage().fetchCurrentURL(url -> {
                    Person person = optionalPerson.get();
                    if(person.createResetPassword()) {
                        personsRepository.save(person);
                        senderUtils.sendMessageFormatted(RESET_MESSAGE_TEMPLATE, person, null, true);
                        event.getSource().getUI().ifPresent(ui -> ui.navigate(Success.class));
                    } else {
                        Notification.show("Das zurücksetzen des Passworts ist nicht möglich").addThemeVariants(NotificationVariant.LUMO_ERROR);
                    }
                });
            } else {
                phoneNumber.setInvalid(true);
                phoneNumber.setErrorMessage("Es wurde kein Benutzer mit dieser Telefonnummer gefunden");
            }
        });
        confirm.setWidthFull();
        inner.add(confirm);
        add(wrapper);
    }

    @Route("request-password-reset/sucess")
    @AnonymousAllowed
    public static class Success extends Div {
        public Success() {
            add(new H1("Sieh jetzt bitte auf WhatsApp nach und klicke den Link an den du erhalten hast. Du kannst dieses Fenster jetzt schließen"));
        }
    }
}