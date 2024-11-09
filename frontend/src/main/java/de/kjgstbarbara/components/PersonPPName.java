package de.kjgstbarbara.components;

import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.avatar.AvatarVariant;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import de.kjgstbarbara.FrontendUtils;
import de.kjgstbarbara.data.Person;

public class PersonPPName extends HorizontalLayout {
    public PersonPPName(Person person) {
        this.setAlignItems(Alignment.CENTER);
        this.setJustifyContentMode(JustifyContentMode.START);
        this.setWidthFull();

        this.add(new NativeLabel());

        Avatar avatarItem = FrontendUtils.getAvatar(person);
        avatarItem.addThemeVariants(AvatarVariant.LUMO_XSMALL);
        this.add(avatarItem);

        NativeLabel name = new NativeLabel(person.getName());
        this.add(name);
    }
}
