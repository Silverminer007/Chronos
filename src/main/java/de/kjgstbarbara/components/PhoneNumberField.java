package de.kjgstbarbara.components;

import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import de.kjgstbarbara.data.Person;

import java.util.Arrays;
import java.util.Locale;

public class PhoneNumberField extends CustomField<Person.PhoneNumber> implements HasSize {

    private final ComboBox<String> countryCode;

    private final IntegerField areaCode;

    private final IntegerField subscriber;

    public PhoneNumberField() {
        HorizontalLayout horizontalLayout = new HorizontalLayout();
        horizontalLayout.setSpacing(true);

        countryCode = new ComboBox<>("Vorwahl");
        countryCode.setWidth("120px");
        countryCode.setItems(Arrays.stream(Locale.getISOCountries()).map(PhoneNumberUtil.getInstance()::getCountryCodeForRegion).map(s -> "+" + s).toList());
        countryCode.setValue("+49");

        areaCode = new IntegerField("Region");
        areaCode.setWidth("120px");
        areaCode.setPlaceholder("Region");

        subscriber = new IntegerField("Nummer");
        subscriber.setPlaceholder("Nummer");

        horizontalLayout.add(this.countryCode, areaCode, subscriber);
        horizontalLayout.setFlexGrow(1.0, subscriber);

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