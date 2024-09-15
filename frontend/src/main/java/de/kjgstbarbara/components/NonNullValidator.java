package de.kjgstbarbara.components;

import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.data.binder.ValueContext;

public class NonNullValidator<T> implements Validator<T> {
    private final String errorMessage;

    public NonNullValidator(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public NonNullValidator() {
        this("Dieses Feld ist erforderlich");
    }

    @Override
    public ValidationResult apply(T t, ValueContext valueContext) {
        return t == null ? ValidationResult.error(errorMessage) : ValidationResult.ok();
    }
}
