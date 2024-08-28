package de.kjgstbarbara.chronos.components;

import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.data.binder.ValueContext;

import java.util.List;

public class ListSizeValidator<T> implements Validator<List<T>> {
    private final int minSizeIncluded;
    private final int maxSizeIncluded;

    public ListSizeValidator(int minSizeIncluded, int maxSizeIncluded) {
        this.minSizeIncluded = minSizeIncluded;
        this.maxSizeIncluded = maxSizeIncluded;
        if(this.maxSizeIncluded < this.minSizeIncluded && maxSizeIncluded >= 0) {
            throw new IllegalArgumentException("The maximum Amount of elements might not be higher than the minimum Amount");
        }
    }

    @Override
    public ValidationResult apply(List<T> ts, ValueContext valueContext) {
        if(ts.size() < this.minSizeIncluded) {
            return ValidationResult.error("Bitte wähle mindestens " + this.minSizeIncluded + " Element(e) aus");
        } else if(ts.size() > this.maxSizeIncluded && this.maxSizeIncluded >= 0) {
            return ValidationResult.error("Bitte wähle maximal " + this.maxSizeIncluded + " Element(e) aus");
        }
        return ValidationResult.ok();
    }
}