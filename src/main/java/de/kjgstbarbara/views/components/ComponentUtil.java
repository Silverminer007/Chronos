package de.kjgstbarbara.views.components;

import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.converter.StringToLongConverter;

import java.util.Locale;

public class ComponentUtil {
    public static Component getPhoneNumber(Binder<? extends HasPhoneNumber> binder) {
        HorizontalLayout phoneNumber = new HorizontalLayout();
        ComboBox<String> regionCode = getRegionCodeSelect();
        binder.forField(regionCode).withValidator((value, valueContext) -> {
            if (value.isEmpty()) {
                return ValidationResult.error("Bitte wähle eine Vorwahl");
            } else {
                return ValidationResult.ok();
            }
        }).bind(HasPhoneNumber::getRegionCode, HasPhoneNumber::setRegionCode);
        phoneNumber.add(regionCode);
        TextField nationalNumber = getNationalNumberField();
        binder.forField(nationalNumber)
                .withConverter(new NationalNumberToStringConverter())
                .bind(HasPhoneNumber::getNationalNumber, HasPhoneNumber::setNationalNumber);
        phoneNumber.add(nationalNumber);
        return phoneNumber;
    }

    public static ComboBox<String> getRegionCodeSelect() {
        ComboBox<String> regionCode = new ComboBox<>("Vorwahl");
        regionCode.setWidth("30%");
        regionCode.setItems(Locale.getISOCountries());
        regionCode.setValue("DE");
        regionCode.setItemLabelGenerator(ComponentUtil::getPhoneNumberDisplay);
        return regionCode;
    }

    public static TextField getNationalNumberField() {
        TextField nationalNumber = new TextField("Telefonnummer");
        nationalNumber.setWidth("70%");
        return nationalNumber;
    }

    private static String getPhoneNumberDisplay(String countryCode) {
        return "+" + PhoneNumberUtil.getInstance().getCountryCodeForRegion(countryCode) + " (" + countryCode + ")";
    }
}
