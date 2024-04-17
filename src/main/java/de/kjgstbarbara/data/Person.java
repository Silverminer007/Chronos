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

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

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
    private LocalDate birthDate;
    private long phoneNumber = 0L;
    private String password;

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
        Optional<StreamResource> pp = FileHelper.getProfileImage(this.getUsername());
        if (pp.isPresent()) {
            avatar.setImageResource(pp.get());
        } else {
            avatar.setColorIndex((int) (id % 7));
        }
        return avatar;
    }

    public AvatarGroup.AvatarGroupItem getAvatarGroupItem() {
        AvatarGroup.AvatarGroupItem avatarGroupItem = new AvatarGroup.AvatarGroupItem(getName());
        Optional<StreamResource> pp = FileHelper.getProfileImage(this.getUsername());
        if (pp.isPresent()) {
            avatarGroupItem.setImageResource(pp.get());
        } else {
            avatarGroupItem.setColorIndex((int) (id % 7));
        }
        return avatarGroupItem;
    }
}