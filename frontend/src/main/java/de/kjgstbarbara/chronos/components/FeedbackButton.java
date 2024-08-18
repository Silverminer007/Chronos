package de.kjgstbarbara.chronos.components;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import de.kjgstbarbara.chronos.data.Feedback;

public class FeedbackButton extends Button {
    private final Icon icon;
    private final Feedback.Status action;

    public FeedbackButton(Feedback.Status action) {
        this(action, false, true);
    }

    public FeedbackButton(Feedback.Status action, boolean showText, boolean enabled) {
        this.action = action;
        this.icon = switch (action) {
            case COMMITTED -> VaadinIcon.THUMBS_UP.create();
            case CANCELLED -> VaadinIcon.THUMBS_DOWN.create();
            case DONTKNOW -> VaadinIcon.QUESTION_CIRCLE.create();
        };

        String text = switch (action) {
            case COMMITTED -> "Bin dabei";
            case CANCELLED -> "Bin raus";
            case DONTKNOW -> "Keine Ahnung";
        };

        this.setIcon(this.icon);
        if(showText) {
            this.setText(text);
        }
        this.setEnabled(enabled);
        this.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        icon.setColor(!enabled ? switch (action) {
            case COMMITTED -> "#00ff00";
            case CANCELLED -> "#ff0000";
            case DONTKNOW -> "#00ffff";
        } : "#cdcdcd");
    }
}