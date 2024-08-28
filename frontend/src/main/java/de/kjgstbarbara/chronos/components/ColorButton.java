package de.kjgstbarbara.chronos.components;

import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.shared.Registration;
import de.kjgstbarbara.chronos.data.Group;

import java.util.ArrayList;
import java.util.List;

public class ColorButton extends Button implements HasValue<ColorButton.ColorButtonValueChangeEvent, String> {
    private final List<ValueChangeListener<? super ColorButtonValueChangeEvent>> valueChangeListeners = new ArrayList<>();
    private final Icon icon;
    private boolean readOnly = false;

    public ColorButton() {
        this.icon = VaadinIcon.CIRCLE.create();
        this.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        this.setIcon(this.icon);
        this.addClickListener(event -> {
            if(!this.readOnly) {
                this.setValue(Group.generateColor());
            }
        });
    }

    @Override
    public void setValue(String s) {
        this.valueChangeListeners.forEach(valueChangeListener -> valueChangeListener.valueChanged(new ColorButtonValueChangeEvent(this, s)));
        icon.setColor(s);
    }

    @Override
    public String getValue() {
        return icon.getColor();
    }

    @Override
    public Registration addValueChangeListener(ValueChangeListener<? super ColorButtonValueChangeEvent> valueChangeListener) {
        this.valueChangeListeners.add(valueChangeListener);
        return () -> this.valueChangeListeners.remove(valueChangeListener);
    }

    @Override
    public void setReadOnly(boolean b) {
        this.readOnly = b;
    }

    @Override
    public boolean isReadOnly() {
        return this.readOnly;
    }

    @Override
    public void setRequiredIndicatorVisible(boolean b) {

    }

    @Override
    public boolean isRequiredIndicatorVisible() {
        return false;
    }

    public static class ColorButtonValueChangeEvent implements ValueChangeEvent<String> {
        private final ColorButton source;
        private final String newValue;

        public ColorButtonValueChangeEvent(ColorButton source, String newValue) {
            this.source = source;
            this.newValue = newValue;
        }


        @Override
        public HasValue<?, String> getHasValue() {
            return this.source;
        }

        @Override
        public boolean isFromClient() {
            return false;
        }

        @Override
        public String getOldValue() {
            return this.source.getValue();
        }

        @Override
        public String getValue() {
            return this.newValue;
        }
    }
}