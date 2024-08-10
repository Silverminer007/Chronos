package de.kjgstbarbara.chronos.views.security;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.login.LoginOverlay;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route("login")
@PageTitle("Login | Chronos")
@AnonymousAllowed
public class LoginView extends VerticalLayout implements BeforeEnterObserver {
    private final LoginOverlay login = new LoginOverlay();

    public LoginView() {
        addClassName("login-view");
        setSizeFull();

        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        LoginI18n i18n = LoginI18n.createDefault();

        LoginI18n.Header i18nHeader = new LoginI18n.Header();
        i18nHeader.setTitle("Chronos");
        i18nHeader.setDescription("Aktuell in Beta (Justus Henze)");
        i18n.setHeader(i18nHeader);

        LoginI18n.Form i18nForm = i18n.getForm();
        i18nForm.setTitle("Anmelden");
        i18nForm.setUsername("Benutzername");
        i18nForm.setPassword("Passwort");
        i18nForm.setSubmit("Anmelden");
        i18nForm.setForgotPassword("Passwort vergessen?");
        i18n.setForm(i18nForm);

        LoginI18n.ErrorMessage i18nErrorMessage = i18n.getErrorMessage();
        i18nErrorMessage.setTitle("Anmelden fehlgeschlagen");
        i18nErrorMessage.setMessage(
                "Der Benutzername oder das Passwort ist falsch");
        i18n.setErrorMessage(i18nErrorMessage);

        login.setI18n(i18n);

        login.setAction("login");
        login.addForgotPasswordListener(event ->
                event.getSource().getUI().ifPresent(ui ->
                        ui.navigate(RequestPasswordResetView.class)));
        Button register = new Button("Account erstellen");
        register.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        register.addClickListener(event -> event.getSource().getUI().ifPresent(ui -> ui.navigate(RegisterView.class)));
        register.setWidthFull();

        login.getFooter().add(register);
        login.setTitle("Chronos");
        add(login);
        login.setOpened(true);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        // inform the user about an authentication error
        if (beforeEnterEvent.getLocation()
                .getQueryParameters()
                .getParameters()
                .containsKey("error")) {
            login.setError(true);
        }
    }
}