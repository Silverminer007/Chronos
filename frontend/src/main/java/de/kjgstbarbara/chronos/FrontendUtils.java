package de.kjgstbarbara.chronos;

import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.avatar.AvatarGroup;
import com.vaadin.flow.server.StreamResource;
import de.kjgstbarbara.chronos.data.Date;
import de.kjgstbarbara.chronos.data.Feedback;
import de.kjgstbarbara.chronos.data.Person;

import java.util.List;

public class FrontendUtils {
    public static Avatar getAvatar(Person person) {
        Avatar avatar = new Avatar(person.getName());
        StreamResource pp = FileHelper.getProfileImage(person.getUsername());
        if (pp != null) {
            avatar.setImageResource(pp);
        } else {
            avatar.setColorIndex((int) (person.getId() % 7));
        }
        return avatar;
    }

    public static AvatarGroup.AvatarGroupItem getAvatarGroupItem(Person person) {
        AvatarGroup.AvatarGroupItem avatarGroupItem = new AvatarGroup.AvatarGroupItem(person.getName());
        StreamResource pp = FileHelper.getProfileImage(person.getUsername());
        if (pp != null) {
            avatarGroupItem.setImageResource(pp);
        } else {
            avatarGroupItem.setColorIndex((int) (person.getId() % 7));
        }
        return avatarGroupItem;
    }

    public static List<AvatarGroup.AvatarGroupItem> getAvatars(Date date, Feedback.Status status) {
        return date.getGroup().getMembers().stream().filter(p -> date.getStatusFor(p).equals(status)).map(FrontendUtils::getAvatarGroupItem).toList();
    }
}
