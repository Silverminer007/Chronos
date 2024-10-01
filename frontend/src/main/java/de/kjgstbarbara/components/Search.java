package de.kjgstbarbara.components;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;

import java.util.function.Consumer;

public class Search extends HorizontalLayout {
    private String search;

    public Search(Consumer<String> searchUpdated) {
        this.setAlignItems(Alignment.CENTER);
        this.setJustifyContentMode(JustifyContentMode.END);

        TextField searchTextField = new TextField();
        searchTextField.addValueChangeListener(event -> {
            this.search = event.getValue();
            searchUpdated.accept(this.search);
        });
        searchTextField.setVisible(false);
        this.add(searchTextField);

        Button searchButton = new Button(VaadinIcon.SEARCH.create());
        searchButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        searchButton.addClickShortcut(Key.KEY_F, KeyModifier.CONTROL);
        searchButton.addClickListener(event -> {
            if (!searchTextField.isVisible()) {
                searchTextField.setVisible(true);
                searchTextField.focus();
                this.search = searchTextField.getValue();
            } else {
                searchTextField.setVisible(false);
                this.search = null;
            }
            searchUpdated.accept(this.search);
        });
        this.add(searchButton);
    }
}