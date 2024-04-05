package de.kjgstbarbara.views.security;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import de.kjgstbarbara.FriendlyError;
import de.kjgstbarbara.whatsapp.WhatsAppUtils;
import de.kjgstbarbara.data.PasswordReset;
import de.kjgstbarbara.data.Person;
import de.kjgstbarbara.service.PasswordResetRepository;
import de.kjgstbarbara.service.PasswordResetService;
import de.kjgstbarbara.service.PersonsRepository;
import de.kjgstbarbara.service.PersonsService;
import de.kjgstbarbara.views.components.LongNumberField;

import java.util.Objects;
import java.util.Optional;

@Route("request-password-reset")
@AnonymousAllowed
public class RequestPasswordResetView extends VerticalLayout {
    public RequestPasswordResetView(PersonsService personsService, PasswordResetService passwordResetService) {
        PersonsRepository personsRepository = personsService.getPersonsRepository();
        PasswordResetRepository passwordResetRepository = passwordResetService.getPasswordResetRepository();
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
        DatePicker datePicker = new DatePicker("Geburtsdatum");
        inner.add(datePicker);
        datePicker.setWidthFull();
        Button confirm = new Button("Bestätigen");
        confirm.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        confirm.addClickShortcut(Key.ENTER);
        confirm.addClickListener(event -> {
            if (phoneNumber.getValue() == null) {
                phoneNumber.setInvalid(true);
                phoneNumber.setErrorMessage("Bitte gib eine Telefonnummer an");
                return;
            }
            if (datePicker.getValue() == null) {
                datePicker.setInvalid(true);
                datePicker.setErrorMessage("Bitte gib dein Geburtsdatum an");
                return;
            }
            Optional<Person> optionalPerson = personsRepository.findByPhoneNumber(phoneNumber.getValue());
            if (optionalPerson.isPresent()) {
                phoneNumber.setInvalid(false);
                if (Objects.equals(optionalPerson.get().getBirthDate(), datePicker.getValue())) {
                    datePicker.setInvalid(false);
                    try {
                        PasswordReset passwordReset = WhatsAppUtils.getInstance().sendPasswordResetMessage(optionalPerson.get(), WhatsAppUtils.RESET);
                        passwordResetRepository.save(passwordReset);
                        event.getSource().getUI().ifPresent(ui -> ui.navigate(Success.class));
                    } catch (FriendlyError e) {
                        phoneNumber.setInvalid(true);
                        phoneNumber.setErrorMessage("Es ist ein Fehler beim versenden der Nachricht aufgetreten");
                        Notification.show(e.getMessage());
                    }
                } else {
                    datePicker.setInvalid(true);
                    datePicker.setErrorMessage("Das Geburtsdatum passt nicht zur eingegebenen Telefonnummer");
                }
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