package de.kjgstbarbara.views.security;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import de.kjgstbarbara.data.Person;
import de.kjgstbarbara.security.SecurityUtils;
import de.kjgstbarbara.service.PersonsRepository;
import de.kjgstbarbara.service.PersonsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

@Route("password-reset/:personId/:token")
@AnonymousAllowed
public class ResetPasswordTokenizedView extends VerticalLayout implements BeforeEnterObserver, AfterNavigationObserver {
    private final PersonsRepository personsRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private Person person;

    private final H1 title = new H1();
    private final TextField username = new TextField("Benutzername");

    public ResetPasswordTokenizedView(PersonsService personsService) {
        this.personsRepository = personsService.getPersonsRepository();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        VerticalLayout wrapper = new VerticalLayout();
        wrapper.setWidth(200, Unit.MM);
        username.setWidthFull();
        PasswordField password = new PasswordField("Passwort");
        password.setWidthFull();
        password.setRequired(true);
        password.addValueChangeListener(event -> {
            boolean validPassword = SecurityUtils.checkPassword(password.getValue());
            password.setInvalid(!validPassword);
            if (validPassword) {
                password.setErrorMessage("");
            } else {
                password.setErrorMessage("Das Passwort muss mindestens 10 Zeichen lang sein");
            }
        });
        PasswordField retypepassword = new PasswordField("Passwort erneut eingeben");
        retypepassword.setWidthFull();
        retypepassword.setRequired(true);
        retypepassword.addValueChangeListener(event -> {
            if (!Objects.equals(event.getValue(), password.getValue())) {
                retypepassword.setErrorMessage("Die Passwörter müssen gleich sein");
                retypepassword.setInvalid(true);
            } else {
                retypepassword.setErrorMessage("");
                retypepassword.setInvalid(false);
            }
        });
        Button confirm = new Button("Bestätigen");
        confirm.setWidthFull();
        confirm.addClickShortcut(Key.ENTER);
        confirm.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        confirm.addClickListener(event -> {
            Optional<Person> personOptional = personsRepository.findByUsername(username.getValue());
            boolean error = false;
            if (username.getValue().isBlank() && person.getUsername().isBlank()) {
                username.setErrorMessage("Der Benutzername darf nicht leer sein");
                username.setInvalid(true);
                error = true;
            } else if (personOptional.isPresent() && !personOptional.get().equals(person)) {
                username.setErrorMessage("Der Benutzername ist bereits vergeben");
                username.setInvalid(true);
                error = true;
            } else {
                username.setErrorMessage("");
                username.setInvalid(false);
            }
            if (!Objects.equals(retypepassword.getValue(), password.getValue())) {
                retypepassword.setErrorMessage("Die Passwörter müssen gleich sein");
                retypepassword.setInvalid(true);
                error = true;
            } else {
                retypepassword.setErrorMessage("");
                retypepassword.setInvalid(false);
            }
            if (!SecurityUtils.checkPassword(password.getValue())) {
                password.setErrorMessage("Das Passwort muss mindestens 10 Zeichen lang sein");
                password.setInvalid(true);
                error = true;
            } else {
                password.setErrorMessage("");
                password.setInvalid(false);
            }
            if(error) {
                return;
            }
            person.setPassword(passwordEncoder.encode(password.getValue()));
            person.setUsername(username.getValue().isBlank()
                    ? person.getUsername() : username.getValue());
            person.setResetTokenExpires(LocalDateTime.now());
            personsRepository.save(person);
            event.getSource().getUI().ifPresent(ui -> ui.navigate(LoginView.class));
        });
        HorizontalLayout pass = new HorizontalLayout(password, retypepassword);
        pass.setWidthFull();
        wrapper.add(title, username, pass, confirm);
        add(wrapper);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        Optional<Person> person = beforeEnterEvent.getRouteParameters().get("personId").map(Long::valueOf).flatMap(personsRepository::findById);
        Optional<String> token = beforeEnterEvent.getRouteParameters().get("token");
        if (person.isEmpty() || token.isEmpty() || !person.get().getResetToken().equals(token.get())) {
            beforeEnterEvent.getUI().navigate(ResetPasswordView.class, new RouteParameters("status", "invalid-token"));
            beforeEnterEvent.rerouteTo(ResetPasswordView.class, new RouteParameters("status", "invalid-token"));
        } else if (person.get().getResetTokenExpires().isBefore(LocalDateTime.now())) {
            beforeEnterEvent.getUI().navigate(ResetPasswordView.class, new RouteParameters("status", "token-expired"));
        } else {
            this.person = person.get();
        }
    }

    @Override
    public void afterNavigation(AfterNavigationEvent afterNavigationEvent) {
        if (person.getUsername().isEmpty()) {
            title.setText("Zugangsdaten erstellen");
            username.setRequired(true);
        } else {
            title.setText("Passwort zurücksetzen");
            username.setRequired(false);
        }
    }
}