package de.chronoslive.entitys;

import de.chronoslive.messaging.Platform;
import jakarta.persistence.*;
import lombok.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

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
    @Embedded
    private PhoneNumber phoneNumber;
    private String eMailAddress;
    private Locale userLocale = Locale.GERMANY;
    private ZoneId timezone = ZoneOffset.UTC;
    private boolean monthOverview = true;
    private CalendarLayout calendarLayout = CalendarLayout.LIST_NEXT;
    @Column(length = 1000000)
    private String profileImage;
    @ElementCollection(fetch = FetchType.EAGER)
    private List<Notification> notifications = new ArrayList<>(
            List.of(
                    new Notification(Platform.EMAIL, 2),
                    new Notification(Platform.EMAIL, 48)
            ));
    private Platform prefferedPlatform;

    public Person(long id) {
        this.id = id;
    }

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

    public ZoneId getTimezone() {
        return timezone == null ? ZoneOffset.UTC : timezone;
    }

    public CalendarLayout getCalendarLayout() {
        return calendarLayout == null ? CalendarLayout.LIST_NEXT : calendarLayout;
    }

    public Platform getPrefferedPlatform() {
        return this.prefferedPlatform == null ? Platform.EMAIL : this.prefferedPlatform;
    }

    public List<Notification> getNotifications() {
        return this.notifications == null ? new ArrayList<>(
                List.of(
                        new Notification(Platform.EMAIL, 2),
                        new Notification(Platform.EMAIL, 48)
                )) : this.notifications;
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

        public static PhoneNumber parse(String phoneNumberString) {
            String[] parts = phoneNumberString.split(" ");
            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid phone number: " + phoneNumberString);
            } else {
                try {
                    String countryCode = parts[0];
                    Integer areaCode = Integer.valueOf(parts[1]);
                    Integer subscriber = Integer.valueOf(parts[2]);
                    return new PhoneNumber(countryCode, areaCode, subscriber);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid phone number: " + phoneNumberString);
                }
            }
        }
    }

    @Getter
    public enum CalendarLayout {
        LIST_PER_MONTH("Liste (Monatsweise)"),
        LIST_PER_YEAR("Liste (Jahresweise)"),
        LIST_NEXT("Liste (immer 20)"),
        MONTH("Kalenderblatt (Monat)"),
        YEAR("Kalenderblatt (Jahr)");


        private final String readableName;

        CalendarLayout(String readableName) {
            this.readableName = readableName;
        }
    }

    @Setter
    @Getter
    @Embeddable
    public static class Notification {
        private Platform platform;
        private int hoursBefore;

        public Notification(Platform platform, int hoursBefore) {
            this.platform = platform;
            this.hoursBefore = hoursBefore;
        }


        public Notification() {
            this.platform = Platform.EMAIL;
            this.hoursBefore = 2;
        }

    }
}