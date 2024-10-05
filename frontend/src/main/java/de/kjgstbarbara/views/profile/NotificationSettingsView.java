package de.kjgstbarbara.views.profile;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.H5;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;
import de.kjgstbarbara.components.Header;
import de.kjgstbarbara.data.Person;
import de.kjgstbarbara.messaging.Platform;
import de.kjgstbarbara.service.PersonsRepository;
import de.kjgstbarbara.service.PersonsService;
import de.kjgstbarbara.views.MainNavigationView;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.List;

@Route(value = "notifications", layout = MainNavigationView.class)
@PageTitle("Benachrichtigungen")
@PermitAll
public class NotificationSettingsView extends VerticalLayout {
    private final PersonsRepository personsRepository;
    private final Person principal;

    public NotificationSettingsView(PersonsService personsService, AuthenticationContext authenticationContext) {
        this.personsRepository = personsService.getPersonsRepository();
        this.principal = authenticationContext.getAuthenticatedUser(UserDetails.class)
                .flatMap(userDetails -> personsRepository.findByUsername(userDetails.getUsername()))
                .orElse(null);
        if (this.principal == null) {
            authenticationContext.logout();
        } else {
            setSizeFull();
            this.setSpacing(false);
            this.setPadding(false);
            this.add(this.createHeader());
            this.add(this.createIntroduction());
            this.add(this.createPreferredPlatform());
            this.add(this.createReminders());
        }
    }

    private Component createHeader() {
        HorizontalLayout header = new Header();

        H4 title = new H4("Benachrichtigungen");
        header.add(title);
        return header;
    }

    private Component createIntroduction() {
        VerticalLayout introduction = new VerticalLayout();
        introduction.setWidthFull();
        introduction.setAlignItems(Alignment.START);
        introduction.add(new NativeLabel("Du kannst über Chronos Terminerinnerungen erhalten. Hier kannst du auswählen wann, wie oft und worüber du deine Benachrichtigungen erhalten möchtest"));
        return introduction;
    }

    private Component createPreferredPlatform() {
        VerticalLayout preferredPlatform = new VerticalLayout();
        preferredPlatform.setWidthFull();
        preferredPlatform.setAlignItems(Alignment.START);

        preferredPlatform.add(new H5("Bevorzugte Platform"));

        preferredPlatform.add(new NativeLabel("Diese Platform wird verwendet wenn du zu einem Termin abstimmen sollst ob du kommst oder Benachrichtigungen vom System erhälst"));

        ComboBox<Platform> platformComboBox = new ComboBox<>();
        platformComboBox.setItems(Platform.values());
        platformComboBox.setItemLabelGenerator(Platform::name);
        platformComboBox.setValue(this.principal.getPrefferedPlatform());
        platformComboBox.addValueChangeListener(event -> {
            this.principal.setPrefferedPlatform(event.getValue());
            personsRepository.save(this.principal);// TODO Die Person ändert sich, sonst speicher Fehler
        });
        preferredPlatform.add(platformComboBox);

        return preferredPlatform;
    }

    private Component createReminders() {
        VerticalLayout reminders = new VerticalLayout();
        reminders.setWidthFull();
        reminders.setAlignItems(Alignment.START);
        reminders.setSpacing(false);
        reminders.setPadding(false);

        VerticalLayout content = new VerticalLayout();
        content.setWidthFull();
        content.setAlignItems(Alignment.START);
        content.setSpacing(true);
        content.setPadding(true);

        content.add(new H5("Termin Erinnerungen"));

        HorizontalLayout buttons = new HorizontalLayout();
        buttons.setWidthFull();
        buttons.setAlignItems(Alignment.CENTER);
        buttons.setJustifyContentMode(JustifyContentMode.START);

        Button add = new Button("Neu", VaadinIcon.PLUS.create());
        add.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        buttons.add(add);

        Button reset = new Button("Zurücksetzen", VaadinIcon.REFRESH.create());
        reset.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        reset.setTooltipText("Zurücksetzen auf Standard Einstellungen");
        buttons.add(reset);

        content.add(buttons);

        reminders.add(content);

        Grid<Person.Notification> grid = new Grid<>(Person.Notification.class, false);
        grid.setItems(this.principal.getNotifications());
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_ROW_STRIPES);

        grid.addComponentColumn(notification -> {
            ComboBox<Platform> platformComboBox = new ComboBox<>();
            platformComboBox.setItems(Platform.values());
            platformComboBox.setItemLabelGenerator(Platform::name);
            platformComboBox.setValue(notification.getPlatform());
            platformComboBox.addValueChangeListener(event -> {
                notification.setPlatform(event.getValue());
                personsRepository.save(this.principal);// TODO Die Person ändert sich, sonst speicher Fehler
            });
            return platformComboBox;
        }).setHeader("Platform");

        grid.addComponentColumn(notification -> {
            IntegerField hoursBeforeField = new IntegerField();
            hoursBeforeField.setValue(notification.getHoursBefore());
            hoursBeforeField.setMin(1);
            hoursBeforeField.setMax(168);
            hoursBeforeField.addValueChangeListener(event -> {
                notification.setHoursBefore(event.getValue());
                personsRepository.save(this.principal);
            });
            return hoursBeforeField;
        }).setHeader("Stunden Vorher");

        grid.addComponentColumn(notification -> {
            Button delete = new Button(VaadinIcon.TRASH.create());
            delete.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY_INLINE);
            delete.addClickListener(event -> {
                this.principal.getNotifications().remove(notification);
                personsRepository.save(this.principal);
                grid.setItems(this.principal.getNotifications());
            });
            return delete;
        }).setFlexGrow(0).setWidth("60px");

        reminders.add(grid);

        add.addClickListener(event -> {
            this.principal.getNotifications().add(new Person.Notification());
            personsRepository.save(this.principal);
            grid.setItems(this.principal.getNotifications());
        });

        reset.addClickListener(event -> {
            this.principal.setNotifications(
                    new ArrayList<>(List.of(
                            new Person.Notification(this.principal.getPrefferedPlatform(), 2),
                            new Person.Notification(this.principal.getPrefferedPlatform(), 48)
                    ))
            );
            personsRepository.save(this.principal);
            grid.setItems(this.principal.getNotifications());
            Notification.show("Benachrichtigungen auf Standard Einstellungen zurückgesetzt");
        });
        return reminders;
    }

}