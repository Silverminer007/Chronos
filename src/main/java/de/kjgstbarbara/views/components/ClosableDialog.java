package de.kjgstbarbara.views.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.theme.lumo.LumoIcon;

public class ClosableDialog extends Dialog {

    public ClosableDialog(Component title) {
        HorizontalLayout header = new HorizontalLayout();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(FlexComponent.Alignment.CENTER);

        header.add(title);

        Button close = new Button(LumoIcon.CROSS.create());
        close.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        close.addClickListener(event -> this.close());
        header.add(close);
        this.add(header);
        this.setCloseOnEsc(true);
        this.setCloseOnOutsideClick(true);
    }

    public ClosableDialog() {
        this(new H3());
    }
}