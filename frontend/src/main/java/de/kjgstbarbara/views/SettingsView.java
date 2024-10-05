package de.kjgstbarbara.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.H5;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.validator.EmailValidator;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.kjgstbarbara.FileHelper;
import de.kjgstbarbara.components.ClosableDialog;
import de.kjgstbarbara.components.Header;
import de.kjgstbarbara.components.PhoneNumberField;
import de.kjgstbarbara.components.ReCaptcha;
import de.kjgstbarbara.data.Group;
import de.kjgstbarbara.data.Organisation;
import de.kjgstbarbara.data.Person;
import de.kjgstbarbara.service.*;
import de.kjgstbarbara.views.MainNavigationView;
import de.kjgstbarbara.views.profile.NotificationSettingsView;
import de.kjgstbarbara.views.profile.ProfileView;
import jakarta.annotation.security.PermitAll;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Route(value = "settings", layout = MainNavigationView.class)
@PageTitle("Einstellungen")
@PermitAll
public class SettingsView extends VerticalLayout {

    public SettingsView(PersonsService personsService, PasswordEncoder passwordEncoder, OrganisationService organisationService, FeedbackService feedbackService, GroupService groupService, AuthenticationContext authenticationContext) {
        PersonsRepository personsRepository = personsService.getPersonsRepository();
        Person person = authenticationContext.getAuthenticatedUser(UserDetails.class)
                .flatMap(userDetails -> personsRepository.findByUsername(userDetails.getUsername()))
                .orElse(null);
        if (person == null) {
            authenticationContext.logout();
        } else {
            this.setSizeFull();
            this.setSpacing(false);
            this.setPadding(false);

            this.add(this.createHeader());
            this.add(this.createContent());
        }
    }

    private Component createHeader() {
        HorizontalLayout header = new Header();

        header.add(new H4("Einstellungen"));
        return header;
    }

    private Component createContent() {
        VerticalLayout content = new VerticalLayout();
        content.setSizeFull();
        content.setSpacing(true);
        content.setPadding(true);

        content.add(this.createLink("Profil", ProfileView.class));
        content.add(this.createLink("Benachrichtigungen", NotificationSettingsView.class));
        return content;
    }

    private Component createLink(String name, Class<? extends Component> view) {
        HorizontalLayout link = new HorizontalLayout();
        link.setWidthFull();
        link.setJustifyContentMode(JustifyContentMode.BETWEEN);
        link.setSpacing(true);
        link.setPadding(true);
        link.addClassName(LumoUtility.Background.PRIMARY_50);
        link.addClassName(LumoUtility.BorderRadius.MEDIUM);

        link.add(new H5(name));

        link.addClickListener(event -> {
            UI.getCurrent().navigate(view);
        });

        link.add(VaadinIcon.ARROW_RIGHT.create());
        return link;
    }
}