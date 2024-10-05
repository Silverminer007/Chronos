package de.kjgstbarbara.components;

import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;

public class Header extends HorizontalLayout {
    public Header() {
        this.setWidthFull();
        this.setAlignItems(Alignment.CENTER);
        this.setJustifyContentMode(JustifyContentMode.BETWEEN);
        this.setPadding(true);
        this.setSpacing(true);
        this.addClassNames(LumoUtility.Background.PRIMARY);
    }
}
