package de.kjgstbarbara.chronos.views;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.validator.EmailValidator;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.spring.security.AuthenticationContext;
import de.kjgstbarbara.chronos.Translator;
import de.kjgstbarbara.chronos.data.Person;
import de.kjgstbarbara.chronos.service.PersonsRepository;
import de.kjgstbarbara.chronos.service.PersonsService;
import de.kjgstbarbara.chronos.components.PhoneNumberField;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

@Route("register")
@PageTitle("Registrieren | Chronos")
@AnonymousAllowed
public class RegisterView extends VerticalLayout implements BeforeEnterObserver {
    private final PersonsRepository personsRepository;
    private final String username;

    public RegisterView(Translator translator, PersonsService personsService, AuthenticationContext authenticationContext) {
        this.username = authenticationContext.getAuthenticatedUser(OidcUser.class).map(oidcUser -> oidcUser.getUserInfo().getEmail()).orElseGet(
                () -> {
                    authenticationContext.logout();
                    return "";
                }
        );
        this.personsRepository = personsService.getPersonsRepository();
        if(this.personsRepository.findByUsername(this.username).isPresent()) {
            UI.getCurrent().navigate("");
        }

        this.setSizeFull();
        this.setAlignItems(Alignment.CENTER);
        this.setJustifyContentMode(JustifyContentMode.CENTER);

        Binder<Person> binder = new Binder<>();
        Person person = new Person();
        person.setUsername(this.username);

        FormLayout layout = new FormLayout();
        layout.setMaxWidth("800px");

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

        PhoneNumberField phoneNumberField = new PhoneNumberField(translator);
        binder.forField(phoneNumberField).bind(Person::getPhoneNumber, Person::setPhoneNumber);
        layout.add(phoneNumberField);

        TextField mailAddress = new TextField("E-Mail Adresse");
        mailAddress.setWidthFull();
        binder.forField(mailAddress)
                .withValidator(new EmailValidator("Diese E-Mail Adresse ist ungültig"))
                .bind(Person::getEMailAddress, Person::setEMailAddress);
        layout.add(mailAddress);

        Button createAccount = new Button("Account erstellen");
        createAccount.addClickShortcut(Key.ENTER);
        createAccount.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        layout.add(createAccount);

        createAccount.addClickListener(event -> {
            try {
                binder.writeBean(person);
                person.setUserLocale(UI.getCurrent().getLocale());
                personsService.getPersonsRepository().save(person);
                UI.getCurrent().navigate("");
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

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        if(this.personsRepository.findByUsername(this.username).isPresent()) {
            beforeEnterEvent.rerouteTo("");
        }
    }
}