package de.kjgstbarbara.chronos.components;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

public class DialogFooter extends HorizontalLayout {
    public DialogFooter(Runnable onCancel, Runnable onConfirm, String confirmText) {
        this.setWidthFull();
        this.setAlignItems(Alignment.CENTER);
        this.setJustifyContentMode(JustifyContentMode.END);

        Button cancel = new Button("Abbrechen");
        cancel.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        cancel.addClickListener(e -> onCancel.run());
        this.add(cancel);

        Button save = new Button(confirmText);
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        save.addClickShortcut(Key.ENTER);
        save.addClickListener(e -> onConfirm.run());
        this.add(save);
    }
}
