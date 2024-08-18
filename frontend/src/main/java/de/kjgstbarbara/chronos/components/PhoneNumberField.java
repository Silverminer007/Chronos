package de.kjgstbarbara.chronos.components;

import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import de.kjgstbarbara.chronos.Translator;
import de.kjgstbarbara.chronos.data.Person;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.Locale;

public class PhoneNumberField extends CustomField<Person.PhoneNumber> implements HasSize {

    private final ComboBox<String> countryCode;

    private final IntegerField areaCode;

    private final IntegerField subscriber;

    public PhoneNumberField(@Autowired Translator translator) {
        HorizontalLayout horizontalLayout = new HorizontalLayout();
        horizontalLayout.setSpacing(true);

        countryCode = new ComboBox<>(translator.translate("phone-number.country-code"));
        countryCode.setWidth("90px");
        countryCode.setItems(Arrays.stream(Locale.getISOCountries()).map(PhoneNumberUtil.getInstance()::getCountryCodeForRegion).map(s -> "+" + s).toList());
        countryCode.setValue("+49");

        areaCode = new IntegerField("Region");
        areaCode.setWidth("120px");
        areaCode.setPlaceholder("Region");

        subscriber = new IntegerField(translator.translate("phone-number.number"));
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