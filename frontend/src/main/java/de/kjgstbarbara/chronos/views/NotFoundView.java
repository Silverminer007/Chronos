package de.kjgstbarbara.chronos.views;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route("not-found")
@AnonymousAllowed
public class NotFoundView extends VerticalLayout {
    public NotFoundView() {
        add(new H1("Die eingegebene URL konnte nicht gefunden werden oder du hast keinen Zugriff darauf"));
    }
}