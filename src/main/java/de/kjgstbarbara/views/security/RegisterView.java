package de.kjgstbarbara.views.security;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import de.kjgstbarbara.data.Person;
import de.kjgstbarbara.service.PersonsService;
import de.kjgstbarbara.views.components.LongNumberField;
import de.kjgstbarbara.views.components.ReCaptcha;
import org.springframework.security.crypto.password.PasswordEncoder;

@Route("register")
@PageTitle("Registrieren | KjG Termine")
@AnonymousAllowed
public class RegisterView extends VerticalLayout {

    public RegisterView(PersonsService personsService, PasswordEncoder passwordEncoder) {
        Binder<Person> binder = new Binder<>();
        Person person = new Person();
        addClassName("login-view");
        setSizeFull();

        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        Button createAccount = new Button("Account erstellen");
        H2 title = new H2("Account erstellen");
        TextField firstName = new TextField("Vorname");
        binder.forField(firstName)
                .withValidator((s, valueContext) -> s.isBlank() ? ValidationResult.error("Der Vorname darf nicht leer sein") : ValidationResult.ok())
                .bind(Person::getFirstName, Person::setFirstName);
        firstName.setRequired(true);
        TextField lastName = new TextField("Nachname");
        binder.forField(lastName)
                .withValidator((s, valueContext) -> s.isBlank() ? ValidationResult.error("Der Nachname darf nicht leer sein") : ValidationResult.ok())
                .bind(Person::getLastName, Person::setLastName);
        lastName.setRequired(true);
        TextField username = new TextField("Benutzername");
        binder.forField(username)
                .withValidator((s, valueContext) -> s.isBlank() ? ValidationResult.error("Der Benutzername darf nicht leer sein") : ValidationResult.ok())
                .withValidator(((s, valueContext) -> s.equals(s.replaceAll("[^a-z]", "")) ? ValidationResult.ok() : ValidationResult.error("Der Benutzername darf nur aus Kleinbuchstaben bestehen")))
                .withValidator((value, context) -> personsService.getPersonsRepository().findByUsername(value).isPresent() ? ValidationResult.error("Der Benutzername ist bereits vergeben") : ValidationResult.ok())
                .bind(Person::getUsername, Person::setUsername);
        username.setRequired(true);
        username.setWidthFull();
        LongNumberField phoneNumber = new LongNumberField("Telefonnummer");
        binder.forField(phoneNumber).bind(Person::getPhoneNumber, Person::setPhoneNumber);
        phoneNumber.setWidthFull();
        DatePicker birthDate = new DatePicker("Geburtsdatum");
        binder.forField(birthDate)
                .withValidator((s, valueContext) -> s == null ? ValidationResult.error("Es ist ein Geburtsdatum erforderlich") : ValidationResult.ok())
                .bind(Person::getBirthDate, Person::setBirthDate);
        birthDate.setRequired(true);
        birthDate.setWidthFull();
        PasswordField password = new PasswordField("Passwort");
        binder.forField(password)
                .withValidator((s, context) -> s.isBlank() ? ValidationResult.error("Das Passwort darf nicht leer sein") : ValidationResult.ok())
                .withValidator((s, valueContext) -> s.length() > 7 ? ValidationResult.ok() : ValidationResult.error("Das Passwort muss mindestens 8 zeichen enthalten"))
                .bind(p -> "", (s, c) -> {
                });
        password.setRequired(true);
        password.setWidthFull();
        PasswordField reTypePassword = new PasswordField("Passwort wiederholen");
        reTypePassword.setRequired(true);
        reTypePassword.setWidthFull();
        binder.forField(reTypePassword)
                .withValidator((s, context) -> s.equals(password.getValue()) ? ValidationResult.ok() : ValidationResult.error("Die Passwörter stimmen nicht überein"))
                .bind(p -> "", (p, value) -> p.setPassword(passwordEncoder.encode(value)));
        ReCaptcha reCaptcha = new ReCaptcha();

        Button back = new Button("zurück");
        back.setWidth("30%");
        back.addClickListener(event -> event.getSource().getUI().ifPresent(ui -> ui.navigate(LoginView.class)));

        createAccount.addClickShortcut(Key.ENTER);
        createAccount.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        createAccount.setWidth("65%");

        createAccount.addClickListener(event -> {
            if (reCaptcha.isValid()) {
                try {
                    binder.writeBean(person);
                } catch (ValidationException e) {
                    personsService.getPersonsRepository().save(person);
                    event.getSource().getUI().ifPresent(ui -> ui.navigate(LoginView.class));
                }
            } else {
                Notification.show("Bitte löse zuerst das Captcha").addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        binder.readBean(person);

        HorizontalLayout name = new HorizontalLayout(firstName, lastName);

        HorizontalLayout buttons = new HorizontalLayout(back, createAccount);
        buttons.setWidthFull();
        buttons.setJustifyContentMode(JustifyContentMode.CENTER);

        VerticalLayout wrapper = new VerticalLayout(title, name, username, birthDate, phoneNumber, password, reTypePassword, reCaptcha, buttons);
        wrapper.setWidth(name.getWidth());

        add(wrapper);
    }
}