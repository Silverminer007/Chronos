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
        ComboBox<String> countryCode = new ComboBox<>("Vorwahl");
        countryCode.setWidth("30%");
        countryCode.setItems(Locale.getISOCountries());
        countryCode.setValue("DE");
        countryCode.setItemLabelGenerator(ComponentUtil::getPhoneNumberDisplay);
        countryCode.getStyle().set("--vaadin-combo-box-overlay-width", "250px");
        binder.forField(countryCode).bind(HasPhoneNumber::getRegionCode, HasPhoneNumber::setRegionCode);
        phoneNumber.add(countryCode);
        TextField countrySpecificNumber = new TextField("Telefonnummer");
        countrySpecificNumber.setWidth("70%");
        binder.forField(countrySpecificNumber).withValidator((value, valueContext) -> {
                    if (countryCode.isEmpty()) {
                        return ValidationResult.error("Bitte wähle eine Vorwahl");
                    } else {
                        return ValidationResult.ok();
                    }
                })
                .withConverter(new StringToLongConverter("Deine Telefonnummer darf nur aus Zahlen bestehen"))
                .bind(HasPhoneNumber::getNationalNumber, HasPhoneNumber::setNationalNumber);
        phoneNumber.add(countrySpecificNumber);// TODO Null soll "" bedeuten schmeist aber fehler
        return phoneNumber;
    }

    private static String getPhoneNumberDisplay(String countryCode) {
        return "+" + PhoneNumberUtil.getInstance().getCountryCodeForRegion(countryCode) + " (" + countryCode + ")";
    }
}
