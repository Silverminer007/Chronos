package de.kjgstbarbara.chronos.views.groups;

import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.avatar.AvatarGroup;
import com.vaadin.flow.component.avatar.AvatarGroupVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.server.StreamResource;
import de.kjgstbarbara.chronos.FrontendUtils;
import de.kjgstbarbara.chronos.components.ClosableDialog;
import de.kjgstbarbara.chronos.data.Organisation;
import de.kjgstbarbara.chronos.data.Person;
import de.kjgstbarbara.chronos.messaging.EMailSender;
import de.kjgstbarbara.chronos.messaging.MessageFormatter;
import de.kjgstbarbara.chronos.messaging.SignalSender;
import de.kjgstbarbara.chronos.service.OrganisationRepository;
import it.auties.whatsapp.api.QrHandler;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.mobile.PhoneNumber;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class EditOrg extends VerticalLayout {
    private static final Logger LOGGER = LogManager.getLogger();
    private Person person;
    private OrganisationRepository organisationRepository;
    private Dialog createCreateDialog() {
        ClosableDialog createNewGroupDialog = new ClosableDialog("Neue Organisation");
        createNewGroupDialog.setMaxWidth("400px");
        TextField name = new TextField("Name der Organisation");
        name.setWidthFull();
        name.focus();
        Button create = new Button("Erstellen");
        create.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        create.addClickShortcut(Key.ENTER);
        create.addClickListener(e -> {
            if (name.getValue().isBlank()) {
                name.setInvalid(true);
                name.setErrorMessage("Dieses Feld ist erforderlich");
            } else {
                Organisation newOrg = new Organisation();
                newOrg.setName(name.getValue());
                newOrg.getMembers().add(person);
                newOrg.setAdmin(person);
                organisationRepository.save(newOrg);
                createNewGroupDialog.close();
            }
        });
        createNewGroupDialog.add(name);
        HorizontalLayout dialogFooter = new HorizontalLayout(create);
        dialogFooter.setWidthFull();
        dialogFooter.setAlignItems(FlexComponent.Alignment.END);
        dialogFooter.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        createNewGroupDialog.getFooter().add(dialogFooter);
        return createNewGroupDialog;
    }


    private Component createPeopleSection(Organisation organisation, Person person) {
        VerticalLayout peopleLayout = new VerticalLayout();
        peopleLayout.setWidthFull();

        HorizontalLayout requests = new HorizontalLayout();
        requests.setWidthFull();
        requests.setAlignItems(FlexComponent.Alignment.CENTER);
        H5 requestsLabel = new H5("Anfragen: ");
        requests.add(requestsLabel);
        if (!organisation.getMembershipRequests().isEmpty()) {
            AvatarGroup requestAvatars = new AvatarGroup();
            requestAvatars.setMaxItemsVisible(7);
            requestAvatars.addThemeVariants(AvatarGroupVariant.LUMO_SMALL);
            requestAvatars.setItems(getRequestAvatars(organisation));
            requests.add(requestAvatars);

            requests.addClickListener(event ->
                    createManageRequestsDialog(organisation, person)
                            .setCloseListener(() -> requestAvatars.setItems(getRequestAvatars(organisation)))
                            .open());
        } else {
            NativeLabel noRequests = new NativeLabel("Keine Anfragen");
            requests.add(noRequests);
        }
        peopleLayout.add(requests);

        HorizontalLayout members = new HorizontalLayout();
        members.setWidthFull();
        members.setAlignItems(FlexComponent.Alignment.CENTER);
        H5 membersLabel = new H5("Mitglieder: ");
        members.add(membersLabel);
        if (!organisation.getMembers().isEmpty()) {
            AvatarGroup membersAvatars = new AvatarGroup();
            membersAvatars.setMaxItemsVisible(7);
            membersAvatars.addThemeVariants(AvatarGroupVariant.LUMO_SMALL);
            membersAvatars.setItems(getMemberAvatars(organisation));
            members.add(membersAvatars);

            members.addClickListener(event ->
                    createEditMembersDialog(organisation, person)
                            .setCloseListener(() -> membersAvatars.setItems(getMemberAvatars(organisation)))
                            .open());
        } else {
            NativeLabel noMembers = new NativeLabel("Keine Mitglieder");
            members.add(noMembers);
        }
        peopleLayout.add(members);

        return peopleLayout;
    }

    private List<AvatarGroup.AvatarGroupItem> getMemberAvatars(Organisation organisation) {
        return organisation.getMembers().stream().map(FrontendUtils::getAvatarGroupItem).toList();
    }

    private List<AvatarGroup.AvatarGroupItem> getRequestAvatars(Organisation organisation) {
        return organisation.getMembershipRequests().stream().map(FrontendUtils::getAvatarGroupItem).toList();
    }

    private ClosableDialog createManageRequestsDialog(Organisation organisation, Person person) {
        ClosableDialog dialog = new ClosableDialog("Beitrittsanfragen");

        Grid<Person> requests = new Grid<>();
        requests.setSelectionMode(Grid.SelectionMode.NONE);
        requests.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_NO_ROW_BORDERS);

        requests.addComponentColumn(p -> {
            HorizontalLayout row = new HorizontalLayout();
            row.setAlignItems(FlexComponent.Alignment.CENTER);

            Avatar avatar = FrontendUtils.getAvatar(p);
            row.add(avatar);

            NativeLabel name = new NativeLabel(p.getName());
            row.add(name);

            return row;
        });
        requests.addComponentColumn(p -> {
            HorizontalLayout row = new HorizontalLayout();

            Button confirm = new Button(VaadinIcon.CHECK.create());
            confirm.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SUCCESS);
            confirm.setEnabled(organisation.getAdmin().equals(person));
            confirm.addClickListener(event -> {
                organisation.getMembershipRequests().remove(p);
                organisation.getMembers().add(p);
                organisationRepository.save(organisation);
                requests.setItems(organisation.getMembershipRequests());
            });
            row.add(confirm);

            Button remove = new Button(VaadinIcon.CLOSE.create());
            remove.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
            remove.setEnabled(organisation.getAdmin().equals(person));
            remove.addClickListener(event -> {
                organisation.getMembershipRequests().remove(p);
                organisationRepository.save(organisation);
                requests.setItems(organisation.getMembershipRequests());
            });
            row.add(remove);

            return row;
        });

        requests.setItems(organisation.getMembershipRequests());

        dialog.add(requests);
        dialog.setWidth("500px");

        return dialog;
    }

    private ClosableDialog createEditMembersDialog(Organisation organisation, Person person) {
        ClosableDialog dialog = new ClosableDialog("Mitglieder");

        Grid<Person> members = new Grid<>();
        members.setSelectionMode(Grid.SelectionMode.NONE);
        members.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_NO_ROW_BORDERS);

        members.addComponentColumn(p -> {
            HorizontalLayout row = new HorizontalLayout();
            row.setAlignItems(FlexComponent.Alignment.CENTER);

            Avatar avatar = FrontendUtils.getAvatar(p);
            row.add(avatar);

            NativeLabel name = new NativeLabel(p.getName());
            row.add(name);

            if (organisation.getAdmin().equals(p)) {
                Icon icon = VaadinIcon.STAR.create();
                icon.setColor("#ffc60a");
                icon.setTooltipText("Admin");
                row.add(icon);
            }

            return row;
        }).setHeader("Name");
        members.addComponentColumn(p -> {
            HorizontalLayout row = new HorizontalLayout();

            Button remove = new Button(VaadinIcon.CLOSE.create());
            remove.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
            remove.setEnabled(person.equals(organisation.getAdmin()) && !organisation.getAdmin().equals(p));
            remove.addClickListener(event -> {
                organisation.getMembers().remove(p);
                organisationRepository.save(organisation);
                members.setItems(organisation.getMembers());
            });
            row.add(remove);

            return row;
        }).setFlexGrow(0).setTextAlign(ColumnTextAlign.END);

        members.setItems(organisation.getMembers());

        dialog.add(members);
        dialog.setWidth("500px");

        return dialog;
    }


    private void createMessageSetupDialog(Organisation organisation, Person.Reminder open) {
        ClosableDialog closableDialog = new ClosableDialog("Absender von Nachrichten");

        TabSheet tabSheet = new TabSheet();
        VerticalLayout whatsapp = new VerticalLayout();
        Image qrCode = new Image("", "");
        NativeLabel qrCodeDescription = new NativeLabel("QR Code wird geladen");
        Whatsapp whatsappAccess = Whatsapp.webBuilder().newConnection(organisation.getIDAsUUID())
                .unregistered(qrCodeString -> {
                    LOGGER.info("Setting up Whatsapp with QR Code: {}", qrCodeString);
                    this.getUI().ifPresent(ui -> ui.access(() -> {
                        qrCode.setSrc(qrHandler(qrCodeString));
                        qrCodeDescription.setText("Bitte Scanne den QR Code um WhatsApp Nachrichten über dein Telefon zu verschicken");
                    }));
                });
        Button reconnect = new Button("Trennen");
        reconnect.addClickListener(event -> {
            whatsappAccess.logout();
            closableDialog.close();
            createMessageSetupDialog(organisation, Person.Reminder.WHATSAPP);
        });
        whatsappAccess.addLoggedInListener(api ->
                api.store().phoneNumber().ifPresent(senderPhoneNumber ->
                        this.getUI().ifPresent(ui ->
                                ui.access(() -> {
                                    qrCodeDescription.setVisible(false);
                                    qrCode.setVisible(false);
                                    whatsapp.add(new H3("WhatsApp Nachrichten werden über +" + senderPhoneNumber + " verschickt"));
                                    whatsapp.add(reconnect);
                                }))));
        whatsappAccess.connect().join();
        if (whatsappAccess.store().chats() != null && !whatsappAccess.store().chats().isEmpty()) {
            qrCodeDescription.setVisible(false);
            qrCode.setVisible(false);
            whatsapp.add(new H3("WhatsApp Nachrichten werden über +" + whatsappAccess.store().phoneNumber().map(PhoneNumber::number).orElse(0L) + " verschickt"));
            whatsapp.add(reconnect);
        }
        whatsapp.add(qrCode);
        whatsapp.add(qrCodeDescription);
        closableDialog.setCloseListener(whatsappAccess::disconnect);

        Tab whatsAppTab = new Tab("WhatsApp");
        tabSheet.add(whatsAppTab, whatsapp);

        VerticalLayout signal = new VerticalLayout();
        if (organisation.getSignalSender() != null && organisation.getSignalSender().getPhoneNumber() != 0) {
            signal.add(new H3("Signal Nachrichten werden über +" + organisation.getSignalSender().getPhoneNumber() + " verschickt"));
            Button signOutSignal = new Button("Abmelden");
            signOutSignal.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            signOutSignal.addClickListener(event -> {
                organisation.getSignalSender().unregister();
                organisation.setSignalSender(null);
                organisationRepository.save(organisation);
                closableDialog.close();
                createMessageSetupDialog(organisation, Person.Reminder.SIGNAL);
            });
            signal.add(signOutSignal);
        } else {
            signal.add("Bitte öffne auf deinem Handy Signal, gehe dort auf \"Einstellungen\" -> \"Gekoppelte Geräte\" -> \"+\" und scanne den unten stehenden QR-Code. Dadurch wird deine Organisation mit deinem Signal Account verknüpft und Terminerinnerungen und ähnliches werden dafür verschickt");
            Image signalQRCode = new Image("", "");
            SignalSender newSignalSender = new SignalSender();
            newSignalSender.register(new SignalRegisterConsumer(UI.getCurrent(), (ui, line) -> {
                if (line.startsWith("sgnl")) {
                    ui.access(() -> signalQRCode.setSrc(qrHandler(line)));
                } else if (line.startsWith("Associated with: +")) {
                    long signalPhoneNumber = Long.parseLong(line.replaceAll("Associated with: +", ""));
                    newSignalSender.setPhoneNumber(signalPhoneNumber);
                    organisation.setSignalSender(newSignalSender);
                    organisationRepository.save(organisation);
                    ui.access(() -> {
                        closableDialog.close();
                        createMessageSetupDialog(organisation, Person.Reminder.SIGNAL);
                    });
                }
                LOGGER.info(line);
            }));
            signal.add(signalQRCode);
        }

        Tab signalTab = new Tab("Signal");
        tabSheet.add(signalTab, signal);

        VerticalLayout email = new VerticalLayout();
        Binder<EMailSender> emailBinder = new Binder<>();
        H1 eMailTitle = new H1("E-Mail Konfiguration");
        email.add(eMailTitle);
        TextField smtpServer = new TextField("SMTP Server");
        emailBinder.forField(smtpServer).bind(EMailSender::getSmtpServer, EMailSender::setSmtpServer);
        email.add(smtpServer);
        IntegerField smtpPort = new IntegerField("SMTP Port");
        emailBinder.forField(smtpPort).bind(EMailSender::getSmtpServerPort, EMailSender::setSmtpServerPort);
        email.add(smtpPort);
        TextField name = new TextField("Name des Absenders");
        emailBinder.forField(name).bind(EMailSender::getSenderName, EMailSender::setSenderName);
        email.add(name);
        TextField senderMailAddress = new TextField("E-Mail Adresse des Absenders");
        emailBinder.forField(senderMailAddress).bind(EMailSender::getSenderEmailAddress, EMailSender::setSenderEmailAddress);
        email.add(senderMailAddress);
        PasswordField smtpPassword = new PasswordField("Password des Absenders");
        emailBinder.forField(smtpPassword).bind(EMailSender::getSmtpPassword, EMailSender::setSmtpPassword);
        email.add(smtpPassword);
        EMailSender emailSender = organisation.getEmailSender();
        emailBinder.readBean(emailSender);
        Button testMail = new Button("Speichern & Test Nachricht schicken");
        email.add(testMail);
        testMail.addClickListener(event -> {
            try {
                emailBinder.writeBean(emailSender);
                organisation.setEmailSender(emailSender);
                organisationRepository.save(organisation);
                MessageFormatter messageFormatter = new MessageFormatter().person(this.person);
                organisation.sendMessageTo(messageFormatter.format(EMAIL_TEST_MESSAGE), this.person, Person.Reminder.EMAIL);
                Notification.show("Die E-Mail Konfiguration wurde erfolgreich gespeichert").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (ValidationException e) {
                Notification.show("Ungültige Konfiguration").addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        Tab emailTab = new Tab("E-Mail");
        tabSheet.add(emailTab, email);

        if (open.equals(Person.Reminder.WHATSAPP)) {
            tabSheet.setSelectedTab(whatsAppTab);
        } else if (open.equals(Person.Reminder.SIGNAL)) {
            tabSheet.setSelectedTab(signalTab);
        } else if (open.equals(Person.Reminder.EMAIL)) {
            tabSheet.setSelectedTab(emailTab);
        }

        closableDialog.add(tabSheet);
        closableDialog.open();
    }

    private StreamResource qrHandler(String qr) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            MatrixToImageWriter.writeToStream(QrHandler.createMatrix(qr, 400, 5), "jpg", os);
            return new StreamResource("QR Code", () -> new ByteArrayInputStream(os.toByteArray()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static final String EMAIL_TEST_MESSAGE =
            """
                    Hey #PERSON_FIRSTNAME,
                    du hast eine neue E-Mail Konfiguration gespeichert.
                    Da du diese E-Mail erhalten hast war Einstellung erfolgreich.
                    """;

    private record SignalRegisterConsumer(UI ui, BiConsumer<UI, String> registerCallback) implements Consumer<String> {
        @Override
        public void accept(String string) {
            registerCallback.accept(ui, string);
        }
    }
}
