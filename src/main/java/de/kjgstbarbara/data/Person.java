package de.kjgstbarbara.data;

import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.avatar.AvatarGroup;
import com.vaadin.flow.server.StreamResource;
import de.kjgstbarbara.FileHelper;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.*;

@Entity
@Data
@NoArgsConstructor
public class Person {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Setter(AccessLevel.PACKAGE)
    private long id;
    private String firstName;
    private String lastName;
    private String username = "";
    private long phoneNumber = 0L;
    private String password;
    private Locale userLocale = Locale.GERMANY;
    private boolean whatsappNotifications;
    // Die Uhrzeit am Tag an der die Person ihre Benachrichtigungen erhält. 19 entspricht also 19:00 Uhr
    private int remindMeTime = 19;
    private boolean monthOverview = true;
    private boolean remindOneWeekBefore = false;
    private boolean remindTwoDaysBefore = false;
    private boolean remindOneDayBefore = true;
    private boolean remindSixHoursBefore = false;
    private boolean remindTwoHoursBefore = false;
    private boolean remindOneHourBefore = true;

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
        if (pp.getWriter() != null) {
            avatar.setImageResource(pp);
        } else {
            avatar.setColorIndex((int) (id % 7));
        }
        return avatar;
    }

    public AvatarGroup.AvatarGroupItem getAvatarGroupItem() {
        AvatarGroup.AvatarGroupItem avatarGroupItem = new AvatarGroup.AvatarGroupItem(getName());
        StreamResource pp = FileHelper.getProfileImage(this.getUsername());
        if (pp.getWriter() != null) {
            avatarGroupItem.setImageResource(pp);
        } else {
            avatarGroupItem.setColorIndex((int) (id % 7));
        }
        return avatarGroupItem;
    }
}