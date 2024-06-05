package de.kjgstbarbara.views;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.NativeLabel;
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
import de.kjgstbarbara.FileHelper;
import de.kjgstbarbara.data.Group;
import de.kjgstbarbara.data.Organisation;
import de.kjgstbarbara.data.Person;
import de.kjgstbarbara.service.*;
import de.kjgstbarbara.views.components.ClosableDialog;
import de.kjgstbarbara.views.components.PhoneNumberField;
import de.kjgstbarbara.views.components.ReCaptcha;
import de.kjgstbarbara.views.nav.MainNavigationView;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Route(value = "profile", layout = MainNavigationView.class)
@PageTitle("Profil")
@PermitAll
public class ProfileView extends VerticalLayout {

    public ProfileView(PersonsService personsService, PasswordEncoder passwordEncoder, OrganisationService organisationService, FeedbackService feedbackService, GroupService groupService, AuthenticationContext authenticationContext) {
        PersonsRepository personsRepository = personsService.getPersonsRepository();
        Person person = authenticationContext.getAuthenticatedUser(UserDetails.class)
                .flatMap(userDetails -> personsRepository.findByUsername(userDetails.getUsername()))
                .orElse(null);
        if (person == null) {
            authenticationContext.logout();
        } else {
            Binder<Person> binder = new Binder<>();
            setSizeFull();

            setAlignItems(Alignment.CENTER);
            setJustifyContentMode(JustifyContentMode.CENTER);

            FormLayout content = new FormLayout();
            content.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1), new FormLayout.ResponsiveStep("500px", 2));

            VerticalLayout profilePic = getProfileImageLayout(person, personsRepository);
            content.add(profilePic);
            content.setColspan(profilePic, 2);

            TextField firstName = new TextField("Vorname");
            firstName.setRequired(true);
            binder.forField(firstName)
                    .withValidator((input, valueContext) ->
                            input.isBlank() ?
                                    ValidationResult.error("Dieses Feld ist erforderlich")
                                    : ValidationResult.ok())
                    .bind(Person::getFirstName, Person::setFirstName);
            content.add(firstName);
            TextField lastName = new TextField("Nachname");
            lastName.setRequired(true);
            binder.forField(lastName)
                    .withValidator((input, valueContext) ->
                            input.isBlank() ?
                                    ValidationResult.error("Dieses Feld ist erforderlich")
                                    : ValidationResult.ok())
                    .bind(Person::getLastName, Person::setLastName);
            content.add(lastName);

            PhoneNumberField phoneNumber = new PhoneNumberField();
            binder.forField(phoneNumber).bind(Person::getPhoneNumber, Person::setPhoneNumber);
            content.add(phoneNumber);

            TextField mailAddress = new TextField("E-Mail Adresse");
            mailAddress.setWidthFull();
            binder.forField(mailAddress)
                    .withValidator(new EmailValidator("Diese E-Mail Adresse ist ungültig"))
                    .bind(Person::getEMailAddress, Person::setEMailAddress);
            content.add(mailAddress);

            add(content);

            Button changePassword = getChangePasswordButton(passwordEncoder, person, personsRepository);
            add(changePassword);

            Button save = new Button("Speichern");
            save.addClickShortcut(Key.ENTER);
            save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            save.addClickListener(event -> {
                try {
                    binder.writeBean(person);
                    personsRepository.save(person);
                    Notification.show("Speichern erfolgreich");
                } catch (ValidationException e) {
                    Notification.show(e.getLocalizedMessage());
                }
            });
            add(save);

            Button deleteAccount = new Button("Account löschen");
            deleteAccount.addThemeVariants(ButtonVariant.LUMO_ERROR);
            deleteAccount.addClickListener(event -> {
                List<Organisation> organisations = organisationService.getOrganisationRepository().findByAdmin(person);
                List<Group> groups = new ArrayList<>(groupService.getGroupRepository().findByAdminsIn(person));
                groups.removeIf(g -> g.getAdmins().size() > 1);
                if(organisations.isEmpty() && groups.isEmpty()) {
                    PasswordField passwordField = new PasswordField();
                    ConfirmDialog confirmDialog = new ConfirmDialog(
                            "Account löschen",
                            "Bist du sicher, dass du deinen Account löschen möchtest? Du kannst deinen Account nicht wiederherstellen und deine Daten werden unwiderruflich gelöscht. Um diese Aktion zu bestätigen, gib bitte dein Passwort erneut ein",
                            "Ja, meinen Account löschen",
                            e -> {
                                if (person.getPassword().equals(passwordEncoder.encode(passwordField.getValue()))) {
                                    personsRepository.delete(person);
                                    feedbackService.getFeedbackRepository().deleteByPerson(person);
                                    organisationService.getOrganisationRepository().findByMembersIn(person).forEach(org ->
                                            org.getMembers().remove(person));
                                    organisationService.getOrganisationRepository().findByMembershipRequestsIn(person).forEach(org ->
                                            org.getMembershipRequests().remove(person));
                                    groupService.getGroupRepository().findByMembersIn(person).forEach(group ->
                                            group.getMembers().remove(person));
                                    groupService.getGroupRepository().findByAdminsIn(person).forEach(group ->
                                            group.getAdmins().remove(person));
                                    authenticationContext.logout();
                                }
                            }
                    );
                    confirmDialog.add(passwordField);
                    confirmDialog.open();
                } else {
                    ClosableDialog closableDialog = new ClosableDialog("Account löschen nicht möglich");
                    StringBuilder labelText = new StringBuilder("Bevor du deinen Account löschen kannst musst du alle deine Administratorrechte an jemand anderen übertragen haben.");
                    if(!organisations.isEmpty()) {
                        labelText.append("Du bist noch in diesem Organisationen Admin:");
                        for(Organisation o : organisations) {
                            labelText.append("\n").append(o.getName());
                        }
                    }
                    if(!groups.isEmpty()) {
                        labelText.append("Du bist noch diesen Gruppen alleiniger Admin:");
                        for(Group g : groups) {
                            labelText.append("\n").append(g.getName());
                        }
                    }
                    NativeLabel label = new NativeLabel(labelText.toString());
                    closableDialog.add(label);
                    closableDialog.open();
                }
            });
            add(deleteAccount);

            binder.readBean(person);
        }
    }

    private static Button getChangePasswordButton(PasswordEncoder passwordEncoder, Person person, PersonsRepository personsRepository) {
        Button changePassword = new Button("Passwort ändern");
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
                if (reCaptcha.isValid()) {
                    if (passwordEncoder.encode(password.getValue()).equals(person.getPassword())) {
                        if (newPassword.getValue().length() >= 8) {
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
        return changePassword;
    }

    private static VerticalLayout getProfileImageLayout(Person person, PersonsRepository personsRepository) {
        StreamResource profileImageStreamResource = FileHelper.getProfileImage(person.getUsername());
        Image profileImage = profileImageStreamResource.getWriter() != null ?
                new Image(profileImageStreamResource, "Profilbild")
                : new Image("/images/no-profile-image.png", "Profilbild");
        profileImage.setWidth("150px");

        MemoryBuffer memoryBuffer = new MemoryBuffer();
        Upload profileUpload = new Upload(memoryBuffer);
        profileUpload.setAcceptedFileTypes("image/png");
        profileUpload.setMaxFileSize(1024 * 1024 * 1024);
        profileUpload.setMaxFiles(1);
        profileUpload.addSucceededListener(event -> {
            InputStream inputStream = memoryBuffer.getInputStream();
            try {
                BufferedImage image = ImageIO.read(inputStream);
                int width = image.getWidth();
                BufferedImage circleBuffer = new BufferedImage(width, width, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2 = circleBuffer.createGraphics();
                g2.setClip(new Ellipse2D.Float(0, 0, width, width));
                g2.drawImage(image.getSubimage(0, 0, Math.min(image.getHeight(), image.getWidth()), Math.min(image.getHeight(), image.getWidth())), 0, 0, width, width, null);
                FileHelper.saveProfileImage(circleBuffer, person.getUsername());
                personsRepository.save(person);
                event.getSource().getUI().ifPresent(ui -> ui.getPage().reload());
            } catch (IOException e) {
                Notification.show("Etwas ist beim lesen/speichern der Datei fehlgeschlagen")
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        profileUpload.addFileRejectedListener(event -> Notification.show(event.getErrorMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR));
        VerticalLayout profilePic = new VerticalLayout(profileImage, profileUpload);
        profilePic.setJustifyContentMode(JustifyContentMode.CENTER);
        profilePic.setAlignItems(Alignment.CENTER);
        return profilePic;
    }
}