package de.kjgstbarbara.views;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.theme.lumo.LumoIcon;
import de.kjgstbarbara.FileHelper;
import de.kjgstbarbara.data.Person;
import de.kjgstbarbara.service.PersonsRepository;
import de.kjgstbarbara.service.PersonsService;
import de.kjgstbarbara.views.components.LongNumberField;
import de.kjgstbarbara.views.components.ReCaptcha;
import de.kjgstbarbara.views.nav.MainNavigationView;
import de.kjgstbarbara.views.security.LoginView;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

@Route(value = "profile", layout = MainNavigationView.class)
@PageTitle("Profil")
@AnonymousAllowed
public class ProfileView extends VerticalLayout {

    public ProfileView(PersonsService personsService, PasswordEncoder passwordEncoder, AuthenticationContext authenticationContext) {
        PersonsRepository personsRepository = personsService.getPersonsRepository();
        Person person = authenticationContext.getAuthenticatedUser(UserDetails.class)
                .flatMap(userDetails -> personsRepository.findByUsername(userDetails.getUsername()))
                .orElse(null);
        if(person == null) {
            authenticationContext.logout();
        }
        Binder<Person> binder = new Binder<>();
        setSizeFull();

        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        Image profileImage = new Image(FileHelper.getProfileImagePath(person.getProfileImage()), "Profilbild");

        MemoryBuffer memoryBuffer = new MemoryBuffer();
        Upload profileUpload = new Upload(memoryBuffer);
        profileUpload.setAcceptedFileTypes("image/**");
        profileUpload.addSucceededListener(event -> {
            Notification.show(event.getFileName() + " Wurde erfolgreich hochgeladen");
            InputStream inputStream = memoryBuffer.getInputStream();
            try {
                BufferedImage image = ImageIO.read(inputStream);
                int width = image.getWidth();
                BufferedImage circleBuffer = new BufferedImage(width, width, BufferedImage.TYPE_INT_RGB);
                Graphics2D g2 = circleBuffer.createGraphics();
                g2.setClip(new Ellipse2D.Float(0, 0, width, width));
                g2.drawImage(image.getSubimage(0, 0, Math.min(image.getHeight(), image.getWidth()), Math.min(image.getHeight(), image.getWidth())), 0, 0, width, width, null);
                FileHelper.saveProfileImage(circleBuffer, person.getUsername());
            } catch (IOException e) {
                Notification.show("Etwas ist beim lesen/speichern der Datei fehlgeschlagen");
            }
        });

        TextField firstName = new TextField("Vorname");
        firstName.setRequired(true);
        binder.forField(firstName)
                .withValidator((input, valueContext) ->
                        input.isBlank() ?
                                ValidationResult.error("Dieses Feld ist erforderlich")
                                : ValidationResult.ok())
                .bind(Person::getFirstName, Person::setFirstName);
        TextField lastName = new TextField("Nachname");
        lastName.setRequired(true);
        binder.forField(lastName)
                .withValidator((input, valueContext) ->
                input.isBlank() ?
                        ValidationResult.error("Dieses Feld ist erforderlich")
                        : ValidationResult.ok())
                .bind(Person::getLastName, Person::setLastName);
        LongNumberField phoneNumber = new LongNumberField("Telefonnummer");
        phoneNumber.setRequired(true);
        phoneNumber.setWidthFull();
        binder.forField(phoneNumber)
                .bind(Person::getPhoneNumber, Person::setPhoneNumber);
        DatePicker birthDate = new DatePicker("Geburtsdatum");
        birthDate.setRequired(true);
        birthDate.setWidthFull();
        binder.forField(birthDate).bind(Person::getBirthDate, Person::setBirthDate);

        Button changePassword = new Button("Passwort ändern");
        changePassword.setWidthFull();
        changePassword.addClickListener(event -> {
            Dialog dialog = new Dialog();
            PasswordField password = new PasswordField("Altes Passwort");
            password.setRequired(true);
            password.setWidthFull();
            PasswordField newPassword = new PasswordField("Neues Passwort");
            newPassword.setRequired(true);
            newPassword.setWidthFull();
            PasswordField reTypePassword = new PasswordField("Neues Passwort wiederholen");
            reTypePassword.setRequired(true);
            reTypePassword.setWidthFull();
            ReCaptcha reCaptcha = new ReCaptcha();
            dialog.add(password, newPassword, reTypePassword, reCaptcha);
            dialog.setHeaderTitle("Passwort ändern");
            Button save = new Button("Speichern");
            save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            save.addClickListener(e -> {
                if(reCaptcha.isValid()) {
                    if(passwordEncoder.encode(password.getValue()).equals(person.getPassword())) {
                        if(newPassword.getValue().length() >= 8) {
                            if (newPassword.getValue().equals(reTypePassword.getValue())) {
                                person.setPassword(passwordEncoder.encode(password.getValue()));
                                personsRepository.save(person);
                                dialog.close();
                                Notification.show("Passwort geändert");
                            } else {
                                reTypePassword.setInvalid(true);
                                reTypePassword.setErrorMessage("Die Passwörter stimmen nicht überein");
                            }
                        } else {
                            newPassword.setInvalid(true);
                            newPassword.setErrorMessage("Das Passwort muss aus mindestens 8 Zeichen bestehen");
                        }
                    } else {
                        password.setInvalid(true);
                        password.setErrorMessage("Das Passwort ist falsch");
                    }
                } else {
                    Notification.show("Bitte löse zuerst das Captcha");
                }
            });
            Button cancel = new Button("Zurück");
            cancel.addClickListener(e -> dialog.close());
            HorizontalLayout cancelLayout = new HorizontalLayout(cancel);
            cancelLayout.setWidth("50%");
            cancelLayout.setJustifyContentMode(JustifyContentMode.START);
            HorizontalLayout saveLayout = new HorizontalLayout(save);
            saveLayout.setWidth("50%");
            saveLayout.setJustifyContentMode(JustifyContentMode.END);
            HorizontalLayout footer = new HorizontalLayout(cancelLayout, saveLayout);
            footer.setWidthFull();
            dialog.getFooter().add(footer);
            dialog.open();
        });

        Button save = new Button("Speichern");
        save.addClickShortcut(Key.ENTER);
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        save.setWidthFull();
        save.addClickListener(event -> {
            try {
                binder.writeBean(person);
                Notification.show("Speichern erfolgreich");
            } catch (ValidationException e) {
                Notification.show(e.getLocalizedMessage());
            }
        });
        binder.readBean(person);

        HorizontalLayout name = new HorizontalLayout(firstName, lastName);

        VerticalLayout form = new VerticalLayout(profileUpload, name, birthDate, phoneNumber);
        form.setHeightFull();
        form.setAlignItems(Alignment.START);
        form.setJustifyContentMode(JustifyContentMode.START);
        Scroller scroller = new Scroller(form);
        scroller.setHeightFull();
        scroller.setScrollDirection(Scroller.ScrollDirection.VERTICAL);
        VerticalLayout wrapper = new VerticalLayout(scroller, changePassword, save);
        wrapper.setJustifyContentMode(JustifyContentMode.END);
        wrapper.setAlignItems(Alignment.END);
        wrapper.setHeightFull();
        wrapper.setWidth(name.getWidth());
        add(wrapper);
    }
}