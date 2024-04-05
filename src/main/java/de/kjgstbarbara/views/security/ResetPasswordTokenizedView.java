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
import de.kjgstbarbara.data.PasswordReset;
import de.kjgstbarbara.data.Person;
import de.kjgstbarbara.security.SecurityUtils;
import de.kjgstbarbara.service.PasswordResetRepository;
import de.kjgstbarbara.service.PasswordResetService;
import de.kjgstbarbara.service.PersonsRepository;
import de.kjgstbarbara.service.PersonsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Objects;
import java.util.Optional;

@Route("password-reset/:token")
@AnonymousAllowed
public class ResetPasswordTokenizedView extends VerticalLayout implements BeforeEnterObserver, AfterNavigationObserver {
    private final PasswordResetRepository passwordResetRepository;
    private final PersonsRepository personsRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private final H1 title = new H1();
    private final TextField username = new TextField("Benutzername");

    public ResetPasswordTokenizedView(PasswordResetService passwordResetService, PersonsService personsService) {
        this.passwordResetRepository = passwordResetService.getPasswordResetRepository();
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
            if (username.getValue().isBlank() && passwordReset.getRequester().getUsername().isBlank()) {
                username.setErrorMessage("Der Benutzername darf nicht leer sein");
                username.setInvalid(true);
                error = true;
            } else if (personOptional.isPresent() && !personOptional.get().equals(passwordReset.getRequester())) {
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
            passwordReset.getRequester().setPassword(passwordEncoder.encode(password.getValue()));
            passwordReset.getRequester().setUsername(username.getValue().isBlank()
                    ? passwordReset.getRequester().getUsername() : username.getValue());
            personsRepository.save(passwordReset.getRequester());
            passwordResetRepository.delete(passwordReset);
            event.getSource().getUI().ifPresent(ui -> ui.navigate(LoginView.class));
        });
        HorizontalLayout pass = new HorizontalLayout(password, retypepassword);
        pass.setWidthFull();
        wrapper.add(title, username, pass, confirm);
        add(wrapper);
    }

    private PasswordReset passwordReset;

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        Optional<PasswordReset> passwordResetOptional = beforeEnterEvent.getRouteParameters().get("token").flatMap(passwordResetRepository::findById);
        if (passwordResetOptional.isEmpty()) {
            beforeEnterEvent.getUI().navigate(ResetPasswordView.class, new RouteParameters("status", "invalid-token"));
            beforeEnterEvent.rerouteTo(ResetPasswordView.class, new RouteParameters("status", "invalid-token"));
        } else if (passwordResetOptional.get().expired()) {
            beforeEnterEvent.getUI().navigate(ResetPasswordView.class, new RouteParameters("status", "token-expired"));
        } else {
            this.passwordReset = passwordResetOptional.get();
        }
    }

    @Override
    public void afterNavigation(AfterNavigationEvent afterNavigationEvent) {
        if (passwordReset.getRequester().getUsername().isEmpty()) {
            title.setText("Zugangsdaten erstellen");
            username.setRequired(true);
        } else {
            title.setText("Passwort zurücksetzen");
            username.setRequired(false);
        }
    }
}