package de.kjgstbarbara.views.components;

import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.data.converter.Converter;

public class NationalNumberToStringConverter implements Converter<String, Long> {
    @Override
    public Result<Long> convertToModel(String s, ValueContext valueContext) {
        try {
            return Result.ok(Long.parseLong(s.replaceAll("[-. ]", "")));
        } catch (NumberFormatException e) {
            return Result.error("Das Nummerformat ist ungültig");
        }
    }

    @Override
    public String convertToPresentation(Long aLong, ValueContext valueContext) {
        return String.valueOf(aLong);
    }
}
