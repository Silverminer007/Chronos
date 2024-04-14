package de.kjgstbarbara.views.security;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import de.kjgstbarbara.data.Person;
import de.kjgstbarbara.service.PersonsService;
import de.kjgstbarbara.views.components.LongNumberField;
import de.kjgstbarbara.views.components.ReCaptcha;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Objects;

@Route("register")
@PageTitle("Register | Date Crisis")
@AnonymousAllowed
public class RegisterView extends VerticalLayout {

    public RegisterView(PersonsService personsService, PasswordEncoder passwordEncoder) {
        addClassName("login-view");
        setSizeFull();

        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        Button createAccount = new Button("Account erstellen");
        createAccount.setEnabled(false);
        H2 title = new H2("Account erstellen");
        TextField firstName = new TextField("Vorname");
        firstName.setRequired(true);
        TextField lastName = new TextField("Nachname");
        lastName.setRequired(true);
        TextField username = new TextField("Benutzername");
        username.setRequired(true);
        username.setWidthFull();
        LongNumberField phoneNumber = new LongNumberField("Telefonnummer");
        phoneNumber.setRequired(true);
        phoneNumber.setWidthFull();
        DatePicker birthDate = new DatePicker("Geburtsdatum");
        birthDate.setRequired(true);
        birthDate.setWidthFull();
        PasswordField password = new PasswordField("Passwort");
        password.setRequired(true);
        password.setWidthFull();
        PasswordField reTypePassword = new PasswordField("Passwort wiederholen");
        reTypePassword.setRequired(true);
        reTypePassword.setWidthFull();
        ReCaptcha reCaptcha = new ReCaptcha();

        Button back = new Button("zurück");
        back.setWidth("30%");
        back.addClickListener(event -> event.getSource().getUI().ifPresent(ui -> ui.navigate(LoginView.class)));

        createAccount.addClickShortcut(Key.ENTER);
        createAccount.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        createAccount.setWidth("65%");

        Runnable validateFields = () -> {
            if (firstName.getValue().isBlank()) {
                firstName.setInvalid(true);
                firstName.setErrorMessage("Dieses Feld ist erforderlich");
                createAccount.setEnabled(false);
                return;
            } else {
                firstName.setInvalid(false);
                firstName.setErrorMessage("");
            }
            if (lastName.getValue().isBlank()) {
                lastName.setInvalid(true);
                lastName.setErrorMessage("Dieses Feld ist erforderlich");
                createAccount.setEnabled(false);
                return;
            } else {
                lastName.setInvalid(false);
                lastName.setErrorMessage("");
            }
            if (username.getValue().isBlank()) {
                username.setInvalid(true);
                username.setErrorMessage("Dieses Feld ist erforderlich");
                createAccount.setEnabled(false);
                return;
            } else if (!username.getValue().matches("[a-z]") || username.getValue().contains(" ")) {
                username.setInvalid(true);
                username.setErrorMessage("Der Benutzername darf nur aus Kleinbuchstaben bestehen");
            } else if (personsService.getPersonsRepository().findByUsername(username.getValue()).isPresent()) {
                username.setInvalid(true);
                username.setErrorMessage("Der Benutzername ist bereits vergeben");
            } else {
                username.setInvalid(false);
                username.setErrorMessage("");
            }
            if (birthDate.getValue() == null) {
                birthDate.setInvalid(true);
                birthDate.setErrorMessage("Dieses Feld ist erforderlich");
                createAccount.setEnabled(false);
                return;
            } else {
                birthDate.setInvalid(false);
                birthDate.setErrorMessage("");
            }
            if (phoneNumber.getValue() == null) {
                phoneNumber.setInvalid(true);
                phoneNumber.setErrorMessage("Dieses Feld ist erforderlich");
                createAccount.setEnabled(false);
                return;
            } else {
                phoneNumber.setInvalid(false);
                phoneNumber.setErrorMessage("");
            }
            if (password.getValue().isBlank() || password.getValue().length() < 8) {
                password.setInvalid(true);
                password.setErrorMessage("Das Passwort darf nicht leer sein und muss mindestens 8 Zeichen enthalten");
                createAccount.setEnabled(false);
                return;
            } else {
                password.setInvalid(false);
                password.setErrorMessage("");
            }
            if (!Objects.equals(reTypePassword.getValue(), password.getValue())) {
                reTypePassword.setInvalid(true);
                reTypePassword.setErrorMessage("Die Passwörter stimmen nicht überein");
                createAccount.setEnabled(false);
                createAccount.setEnabled(false);
                return;
            } else {
                reTypePassword.setInvalid(false);
                reTypePassword.setErrorMessage("");
            }
            createAccount.setEnabled(true);
        };
        firstName.addValueChangeListener(event -> validateFields.run());
        lastName.addValueChangeListener(event -> validateFields.run());
        username.addValueChangeListener(event -> validateFields.run());
        birthDate.addValueChangeListener(event -> validateFields.run());
        phoneNumber.addValueChangeListener(event -> validateFields.run());
        password.addValueChangeListener(event -> validateFields.run());
        reTypePassword.addValueChangeListener(event -> validateFields.run());

        createAccount.addClickListener(event -> {
            validateFields.run();
            if (createAccount.isEnabled()) {
                if (reCaptcha.isValid()) {
                    Person newPerson = new Person();
                    newPerson.setFirstName(firstName.getValue());
                    newPerson.setLastName(lastName.getValue());
                    newPerson.setUsername(username.getValue());
                    newPerson.setBirthDate(birthDate.getValue());
                    newPerson.setPhoneNumber(phoneNumber.getValue());
                    newPerson.setPassword(passwordEncoder.encode(password.getValue()));
                    personsService.getPersonsRepository().save(newPerson);
                    event.getSource().getUI().ifPresent(ui -> ui.navigate(LoginView.class));
                } else {
                    createAccount.setAriaLabel("Bitte löse zuerst das Captcha");
                }
            }
        });

        HorizontalLayout name = new HorizontalLayout(firstName, lastName);

        HorizontalLayout buttons = new HorizontalLayout(back, createAccount);
        buttons.setWidthFull();
        buttons.setJustifyContentMode(JustifyContentMode.CENTER);

        VerticalLayout wrapper = new VerticalLayout(title, name, username, birthDate, phoneNumber, password, reTypePassword, reCaptcha, buttons);
        wrapper.setWidth(name.getWidth());
        add(wrapper);
    }
}