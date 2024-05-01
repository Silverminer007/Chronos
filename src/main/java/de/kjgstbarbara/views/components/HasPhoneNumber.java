package de.kjgstbarbara.views.components;

import com.google.i18n.phonenumbers.PhoneNumberUtil;
import de.kjgstbarbara.service.ConfigService;

import java.util.Objects;

public interface HasPhoneNumber {
    void setRegionCode(String string);
    String getRegionCode();
    void setNationalNumber(long nationalNumber);
    long getNationalNumber();

    default long phoneNumber() {
        return Long.parseLong(PhoneNumberUtil.getInstance().getCountryCodeForRegion(this.getRegionCode()) + "" + this.getNationalNumber());
    }

    class Config implements HasPhoneNumber{
        private final ConfigService configService;

        public Config(ConfigService configService) {
            this.configService = configService;
        }

        @Override
        public void setRegionCode(String string) {
            configService.save(de.kjgstbarbara.data.Config.Key.SENDER_REGION_CODE, string);
        }

        @Override
        public String getRegionCode() {
            String regionCode = configService.get(de.kjgstbarbara.data.Config.Key.SENDER_REGION_CODE);
            return Objects.equals(regionCode, "") || regionCode == null ? "DE" : regionCode;
        }

        @Override
        public void setNationalNumber(long nationalNumber) {
            configService.save(de.kjgstbarbara.data.Config.Key.SENDER_NATIONAL_NUMBER, nationalNumber);
        }

        @Override
        public long getNationalNumber() {
            return configService.getLong(de.kjgstbarbara.data.Config.Key.SENDER_NATIONAL_NUMBER);
        }
    }
}
