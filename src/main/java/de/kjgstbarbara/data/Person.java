package de.kjgstbarbara.data;

import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.avatar.AvatarGroup;
import com.vaadin.flow.server.StreamResource;
import de.kjgstbarbara.FileHelper;
import de.kjgstbarbara.security.SecurityUtils;
import jakarta.persistence.*;
import lombok.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Data
@NoArgsConstructor
public class Person {
    private static final Logger LOGGER = LoggerFactory.getLogger(Person.class);
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Setter(AccessLevel.PACKAGE)
    private long id;
    private String firstName;
    private String lastName;
    private String username = "";
    private String password;
    private boolean darkMode = true;
    private Reminder reminder = Reminder.WHATSAPP;
    @Embedded
    private PhoneNumber phoneNumber;
    private String eMailAddress;
    private Locale userLocale = Locale.GERMANY;
    // Die Uhrzeit am Tag an der die Person ihre Benachrichtigungen erhält. 19 entspricht also 19:00 Uhr
    private Set<Integer> remindMeTime = Set.of(19);
    private boolean monthOverview = true;
    private Set<Integer> hourReminderIntervals = Set.of(1);
    private Set<Integer> dayReminderIntervals = Set.of(2);
    private String resetToken;
    private LocalDateTime resetTokenExpires;

    public String toString() {
        return firstName + " " + lastName;
    }


    public int hashCode() {
        return Objects.hashCode(this.getId());
    }

    public boolean equals(Object o) {
        if (o instanceof Person p) {
            return Objects.equals(p.getId(), this.getId());
        }
        return false;
    }

    public String getName() {
        return this.getFirstName() + " " + this.getLastName();
    }

    public Avatar getAvatar() {
        Avatar avatar = new Avatar(getName());
        StreamResource pp = FileHelper.getProfileImage(this.getUsername());
        if (pp != null) {
            avatar.setImageResource(pp);
        } else {
            avatar.setColorIndex((int) (id % 7));
        }
        return avatar;
    }

    public AvatarGroup.AvatarGroupItem getAvatarGroupItem() {
        AvatarGroup.AvatarGroupItem avatarGroupItem = new AvatarGroup.AvatarGroupItem(getName());
        StreamResource pp = FileHelper.getProfileImage(this.getUsername());
        if (pp != null) {
            avatarGroupItem.setImageResource(pp);
        } else {
            avatarGroupItem.setColorIndex((int) (id % 7));
        }
        return avatarGroupItem;
    }

    public boolean createResetPassword() {
        try {
            this.setResetToken(SecurityUtils.generatePassword(200));
            this.setResetTokenExpires(LocalDateTime.now().plusHours(8));
            return true;
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("Failed to generate Password Reset Token", e);
            return false;
        }
    }

    public record PhoneNumber(String countryCode, Integer areaCode, Integer subscriber) {
        public long number() {
            try {
                return Long.parseLong(countryCode() + areaCode() + subscriber());
            } catch (NumberFormatException e) {
                return 0;
            }
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof PhoneNumber phoneNumber && phoneNumber.number() == this.number();
        }

        public String toString() {
            return countryCode + " " + areaCode + " " + subscriber;
        }

        public PhoneNumber(String s) {
            this(splitPhoneNumber(s, 0), Integer.valueOf(splitPhoneNumber(s, 1)), Integer.valueOf(splitPhoneNumber(s, 2)));
        }

        private static String splitPhoneNumber(String phoneNumber, int index) {
            String[] parts = phoneNumber.split(" ");
            if(parts.length <= index) {
                return "0";
            } else {
                return parts[index];
            }
        }
    }

    @Getter
    public enum Reminder {
        WHATSAPP("WhatsApp"), EMAIL("E-Mail");

        private final String text;

        Reminder(String text) {
            this.text = text;
        }
    }
}