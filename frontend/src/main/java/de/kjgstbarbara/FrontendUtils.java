package de.kjgstbarbara;

import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.avatar.AvatarGroup;
import com.vaadin.flow.server.StreamResource;
import de.kjgstbarbara.data.Date;
import de.kjgstbarbara.data.Feedback;
import de.kjgstbarbara.data.Person;

import java.util.List;

public class FrontendUtils {
    public static Avatar getAvatar(Person person) {
        Avatar avatar = new Avatar(person.getName());
        StreamResource pp = FileHelper.getProfileImage(person);
        avatar.setImageResource(pp);
        return avatar;
    }

    public static AvatarGroup.AvatarGroupItem getAvatarGroupItem(Person person) {
        AvatarGroup.AvatarGroupItem avatarGroupItem = new AvatarGroup.AvatarGroupItem(person.getName());
        StreamResource pp = FileHelper.getProfileImage(person);
        avatarGroupItem.setImageResource(pp);
        return avatarGroupItem;
    }

    public static List<AvatarGroup.AvatarGroupItem> getAvatars(Date date, Feedback.Status status) {
        return date.getGroup().getMembers().stream().filter(p -> date.getStatusFor(p).equals(status)).map(FrontendUtils::getAvatarGroupItem).toList();
    }
}
