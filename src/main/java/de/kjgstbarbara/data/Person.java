package de.kjgstbarbara.data;

import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.avatar.AvatarGroup;
import com.vaadin.flow.server.StreamResource;
import de.kjgstbarbara.FileHelper;
import de.kjgstbarbara.security.SecurityUtils;
import de.kjgstbarbara.views.components.HasPhoneNumber;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Data
@NoArgsConstructor
public class Person implements HasPhoneNumber {
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
    private boolean systemAdmin = false;
    private String regionCode;
    private long nationalNumber;
    private boolean whatsappNotifications;
    private String eMailAddress;
    private boolean eMailNotifications;
    private Locale userLocale = Locale.GERMANY;
    // Die Uhrzeit am Tag an der die Person ihre Benachrichtigungen erhält. 19 entspricht also 19:00 Uhr
    private int remindMeTime = 19;
    private boolean monthOverview = true;
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
}