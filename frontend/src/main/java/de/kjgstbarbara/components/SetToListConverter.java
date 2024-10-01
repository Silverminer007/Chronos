package de.kjgstbarbara.components;

import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.data.converter.Converter;
import de.kjgstbarbara.data.Person;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SetToListConverter implements Converter<Set<Person>, List<Person>> {
    @Override
    public Result<List<Person>> convertToModel(Set<Person> people, ValueContext valueContext) {
        return Result.ok(people.stream().toList());
    }

    @Override
    public Set<Person> convertToPresentation(List<Person> people, ValueContext valueContext) {
        return new HashSet<>(people);
    }
}
