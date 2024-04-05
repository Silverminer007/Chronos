package de.kjgstbarbara.views.security;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route("reset-password/:status?")
@AnonymousAllowed
public class ResetPasswordView extends VerticalLayout implements BeforeEnterObserver, AfterNavigationObserver {
    private boolean invalidToken;
    private boolean tokenExpired;
    private final H3 invalidTokenInfo = new H3("Das Token war ungültig, bitte gib ein neues ein");
    private final H3 tokenExpiredInfo = new H3("Das Token ist abgelaufen. Bitte setze dein Password erneut zurück");
    public ResetPasswordView() {
        add(invalidTokenInfo);
        add(tokenExpiredInfo);
        add(new H1("Bitte gib den Code ein, den du zum Zurücksetzen deines Passwords erhalten hast"));
        TextField tokenField = new TextField("");
        tokenField.setAutofocus(true);
        tokenField.setWidthFull();
        tokenField.setRequired(true);
        add(tokenField);
        Button confirm = new Button("Bestätigen");
        confirm.addClickShortcut(Key.ENTER);
        confirm.addClickListener(event ->
                confirm.getUI().ifPresent(ui ->
                        ui.navigate(ResetPasswordTokenizedView.class,
                                new RouteParameters(new RouteParam("token", tokenField.getValue())))));

    }
    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        invalidToken = beforeEnterEvent.getRouteParameters().get("status").map(s -> s.equalsIgnoreCase("invalid-token")).orElse(false);
        tokenExpired = beforeEnterEvent.getRouteParameters().get("status").map(s -> s.equalsIgnoreCase("token-expired")).orElse(false);
    }

    @Override
    public void afterNavigation(AfterNavigationEvent afterNavigationEvent) {
        invalidTokenInfo.setVisible(invalidToken);
        tokenExpiredInfo.setVisible(tokenExpired);
    }
}