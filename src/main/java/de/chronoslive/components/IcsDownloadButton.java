package de.chronoslive.components;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;

public class IcsDownloadButton extends Anchor {
    public IcsDownloadButton(String type, long id) {
        super("/api/v1/ical/" + type + "/" + id + ".ics", createDownloadIcsAnchor());
        this.getElement().setAttribute("download", true);
    }

    private static Button createDownloadIcsAnchor() {
        Button button = new Button(VaadinIcon.DOWNLOAD.create());
        button.addClickListener(event ->
                Notification.show("Der Download wurde gestartet, bitte hab einen Moment Geduld"));
        button.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        return button;
    }
}