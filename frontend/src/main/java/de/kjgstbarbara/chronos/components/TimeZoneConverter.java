package de.kjgstbarbara.chronos.components;

import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.data.converter.Converter;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

public class TimeZoneConverter implements Converter<LocalDateTime, LocalDateTime> {
    private final ZoneId timezone;

    public TimeZoneConverter(ZoneId timezone) {
        this.timezone = timezone;
    }

    @Override
    public Result<LocalDateTime> convertToModel(LocalDateTime localDateTime, ValueContext valueContext) {
        return localDateTime == null ? Result.error("No Day and Time specified") : Result.ok(localDateTime.atZone(timezone).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime());
    }

    @Override
    public LocalDateTime convertToPresentation(LocalDateTime localDateTime, ValueContext valueContext) {
        return localDateTime == null ? null : localDateTime.atZone(ZoneOffset.UTC).withZoneSameInstant(timezone).toLocalDateTime();
    }
}
