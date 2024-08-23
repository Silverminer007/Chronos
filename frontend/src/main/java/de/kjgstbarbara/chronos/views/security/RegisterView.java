package de.kjgstbarbara.chronos.views.security;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.validator.EmailValidator;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import de.kjgstbarbara.chronos.Translator;
import de.kjgstbarbara.chronos.data.Person;
import de.kjgstbarbara.chronos.service.PersonsService;
import de.kjgstbarbara.chronos.components.PhoneNumberField;
import org.springframework.security.crypto.password.PasswordEncoder;

@Route("register")
@PageTitle("Registrieren | Chronos")
@AnonymousAllowed
public class RegisterView extends VerticalLayout {

    public RegisterView(Translator translator, PersonsService personsService, PasswordEncoder passwordEncoder) {
        Binder<Person> binder = new Binder<>();
        Person person = new Person();
        addClassName("login-view");
        setSizeFull();

        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        FormLayout layout = new FormLayout();

        H2 title = new H2("Account erstellen");
        layout.add(title);
        layout.setColspan(title, 2);
        TextField firstName = new TextField("Vorname");
        binder.forField(firstName)
                .withValidator((s, valueContext) -> s.isBlank() ? ValidationResult.error("Der Vorname darf nicht leer sein") : ValidationResult.ok())
                .bind(Person::getFirstName, Person::setFirstName);
        firstName.setRequired(true);
        layout.add(firstName);
        TextField lastName = new TextField("Nachname");
        binder.forField(lastName)
                .withValidator((s, valueContext) -> s.isBlank() ? ValidationResult.error("Der Nachname darf nicht leer sein") : ValidationResult.ok())
                .bind(Person::getLastName, Person::setLastName);
        lastName.setRequired(true);
        layout.add(lastName);
        TextField username = new TextField("Benutzername");
        binder.forField(username)
                .withValidator((s, valueContext) -> s.isBlank() ? ValidationResult.error("Der Benutzername darf nicht leer sein") : ValidationResult.ok())
                .withValidator(((s, valueContext) -> s.equals(s.replaceAll("[^a-z]", "")) ? ValidationResult.ok() : ValidationResult.error("Der Benutzername darf nur aus Kleinbuchstaben bestehen")))
                .withValidator((value, context) -> personsService.getPersonsRepository().findByUsername(value).isPresent() ? ValidationResult.error("Der Benutzername ist bereits vergeben") : ValidationResult.ok())
                .bind(Person::getUsername, Person::setUsername);
        username.setRequired(true);
        username.setWidthFull();
        layout.add(username);
        layout.setColspan(username, 2);

        PhoneNumberField phoneNumberField = new PhoneNumberField(translator);
        binder.forField(phoneNumberField).bind(Person::getPhoneNumber, Person::setPhoneNumber);
        layout.add(phoneNumberField);

        TextField mailAddress = new TextField("E-Mail Adresse");
        mailAddress.setWidthFull();
        binder.forField(mailAddress)
                .withValidator(new EmailValidator("Diese E-Mail Adresse ist ungültig"))
                .bind(Person::getEMailAddress, Person::setEMailAddress);
        layout.add(mailAddress);

        PasswordField password = new PasswordField("Passwort erstellen");
        binder.forField(password)
                .withValidator((s, context) -> s.isBlank() ? ValidationResult.error("Das Passwort darf nicht leer sein") : ValidationResult.ok())
                .withValidator((s, valueContext) -> s.length() > 7 ? ValidationResult.ok() : ValidationResult.error("Das Passwort muss mindestens 8 zeichen enthalten"))
                .bind(p -> "", (s, c) -> {
                });
        password.setRequired(true);
        password.setWidthFull();
        layout.add(password);
        PasswordField reTypePassword = new PasswordField("Passwort wiederholen");
        reTypePassword.setRequired(true);
        reTypePassword.setWidthFull();
        binder.forField(reTypePassword)
                .withValidator((s, context) -> s.equals(password.getValue()) ? ValidationResult.ok() : ValidationResult.error("Die Passwörter stimmen nicht überein"))
                .bind(p -> "", (p, value) -> p.setPassword(passwordEncoder.encode(value)));
        layout.add(reTypePassword);

        Button back = new Button("zurück");
        back.setWidth("30%");
        back.addClickListener(event -> event.getSource().getUI().ifPresent(ui -> ui.navigate(LoginView.class)));
        layout.add(back);

        Button createAccount = new Button("Account erstellen");
        createAccount.addClickShortcut(Key.ENTER);
        createAccount.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        createAccount.setWidth("65%");
        layout.add(createAccount);

        createAccount.addClickListener(event -> {
            try {
                binder.writeBean(person);
                person.setUserLocale(UI.getCurrent().getLocale());
                personsService.getPersonsRepository().save(person);
                UI.getCurrent().navigate(LoginView.class);
            } catch (ValidationException e) {
                Notification.show("Ein Fehler ist ausgetreten, der Account konnte nicht erstellt werdne").addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        binder.readBean(person);

        layout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("600px", 2));
        add(layout);
    }
}