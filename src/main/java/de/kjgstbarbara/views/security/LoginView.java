package de.kjgstbarbara.views.security;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.login.LoginOverlay;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route("login")
@PageTitle("Login | Vaadin CRM")
@AnonymousAllowed
public class LoginView extends VerticalLayout implements BeforeEnterObserver {
    private final LoginOverlay login = new LoginOverlay();

    public LoginView() {
        addClassName("login-view");
        setSizeFull();

        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        login.setAction("login");
        login.addForgotPasswordListener(event ->
                event.getSource().getUI().ifPresent(ui ->
                        ui.navigate(RequestPasswordResetView.class)));
        Button register = new Button("Register");
        register.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        register.addClickListener(event -> event.getSource().getUI().ifPresent(ui -> ui.navigate(RegisterView.class)));
        register.setWidthFull();

        login.getFooter().add(register);
        login.setTitle("Date Crisis");
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