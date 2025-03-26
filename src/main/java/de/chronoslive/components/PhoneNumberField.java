package de.chronoslive.components;

import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import de.chronoslive.entitys.Person;

import java.util.List;

public class PhoneNumberField extends CustomField<Person.PhoneNumber> implements HasSize {

    private final ComboBox<String> countryCode;

    private final IntegerField areaCode;

    private final IntegerField subscriber;

    public PhoneNumberField() {
        HorizontalLayout horizontalLayout = new HorizontalLayout();
        horizontalLayout.setSpacing(true);

        countryCode = new ComboBox<>("Country Code");
        countryCode.setWidth("90px");
        countryCode.setItems(List.of("+49"));
        countryCode.setValue("+49");

        areaCode = new IntegerField("Region");
        areaCode.setWidth("90px");
        areaCode.setPlaceholder("Region");

        subscriber = new IntegerField("Nummer");
        subscriber.setPlaceholder("Nummer");
        subscriber.setMaxWidth("120px");

        horizontalLayout.add(this.countryCode, areaCode, subscriber);
        horizontalLayout.setWidth("300px");
        //horizontalLayout.setFlexGrow(1.0, subscriber);

        add(horizontalLayout);
    }

    @Override
    protected Person.PhoneNumber generateModelValue() {
        return new Person.PhoneNumber(countryCode.getValue(), areaCode.getValue(), subscriber.getValue());
    }

    @Override
    protected void setPresentationValue(Person.PhoneNumber value) {
        countryCode.setValue(value.countryCode());
        areaCode.setValue(value.areaCode());
        subscriber.setValue(value.subscriber());
    }

}