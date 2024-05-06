package de.kjgstbarbara.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.avatar.AvatarGroup;
import com.vaadin.flow.component.avatar.AvatarGroupVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H5;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;
import de.kjgstbarbara.data.Feedback;
import de.kjgstbarbara.data.Group;
import de.kjgstbarbara.data.Person;
import de.kjgstbarbara.service.*;
import de.kjgstbarbara.views.components.ClosableDialog;
import de.kjgstbarbara.views.nav.MainNavigationView;
import jakarta.annotation.security.PermitAll;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.userdetails.UserDetails;
import org.vaadin.olli.ClipboardHelper;

import java.util.List;
import java.util.stream.Stream;

@Route(value = "groups", layout = MainNavigationView.class)
@PageTitle("Gruppen")
@PermitAll
public class GroupView extends VerticalLayout {
    private final PersonsRepository personsRepository;
    private final GroupRepository groupRepository;
    private final DateRepository dateRepository;
    private final FeedbackRepository feedbackRepository;

    private final Grid<Group> grid = new Grid<>(Group.class, false);
    private final TextField search = new TextField();

    private final Person person;

    public GroupView(PersonsService personsService, GroupService groupService, DatesService datesService, FeedbackService feedbackService, AuthenticationContext authenticationContext) {
        this.personsRepository = personsService.getPersonsRepository();
        this.groupRepository = groupService.getGroupRepository();
        this.dateRepository = datesService.getDateRepository();
        this.feedbackRepository = feedbackService.getFeedbackRepository();
        this.person = authenticationContext.getAuthenticatedUser(UserDetails.class)
                .flatMap(userDetails -> personsRepository.findByUsername(userDetails.getUsername()))
                .orElse(null);
        if (person == null) {
            authenticationContext.logout();
        }

        this.setHeightFull();

        HorizontalLayout header = new HorizontalLayout();
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);
        header.setWidthFull();
        HorizontalLayout filter = new HorizontalLayout();
        filter.setAlignItems(Alignment.CENTER);
        NativeLabel searchLabel = new NativeLabel("Suche: ");
        filter.add(searchLabel);
        search.setValueChangeMode(ValueChangeMode.LAZY);
        filter.add(search);
        header.add(filter);
        Button add = new Button("Neue Gruppe", VaadinIcon.PLUS_SQUARE_O.create());
        header.add(add);
        this.add(header);

        this.add(new Hr());

        grid.setHeightFull();
        grid.addComponentColumn(group -> createGroupWidget(group, this.person));
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_ROW_STRIPES);
        grid.setSelectionMode(Grid.SelectionMode.NONE);
        grid.setItems(this::updateGrid);
        search.addValueChangeListener(event -> grid.setItems(this::updateGrid));

        add.addClickListener(event -> {
            ClosableDialog createNewGroupDialog = new ClosableDialog("Neue Gruppe");
            TextField name = new TextField("Name der Gruppe");
            name.focus();
            Button create = new Button("Erstellen");
            create.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            create.addClickShortcut(Key.ENTER);
            create.addClickListener(e -> {
                if (name.getValue().isBlank()) {
                    name.setInvalid(true);
                    name.setErrorMessage("Dieses Feld ist erforderlich");
                } else {
                    Group newGroup = new Group();
                    newGroup.setTitle(name.getValue());
                    newGroup.getMembers().add(person);
                    newGroup.getAdmins().add(person);
                    groupRepository.save(newGroup);
                    grid.setItems(this::updateGrid);
                    createNewGroupDialog.close();
                }
            });
            createNewGroupDialog.add(name);
            HorizontalLayout dialogFooter = new HorizontalLayout(create);
            dialogFooter.setWidthFull();
            dialogFooter.setAlignItems(Alignment.END);
            dialogFooter.setJustifyContentMode(JustifyContentMode.END);
            createNewGroupDialog.getFooter().add(dialogFooter);
            createNewGroupDialog.open();
        });

        this.add(grid);
    }

    private Stream<Group> updateGrid(Query<Group, Void> query) {
        return groupRepository.findByTitleIgnoreCaseLike(
                "%" + search.getValue() + "%",
                PageRequest.of(query.getPage(),
                        query.getPageSize())
        ).stream();
    }

    private Component createGroupWidget(Group group, Person person) {
        VerticalLayout groupWidget = new VerticalLayout();

        HorizontalLayout summary = new HorizontalLayout();
        summary.setAlignItems(Alignment.CENTER);
        summary.setWidthFull();

        VerticalLayout collapsableArea = new VerticalLayout();
        collapsableArea.setVisible(false);

        Icon colorCircle = VaadinIcon.CIRCLE.create();
        colorCircle.setColor(group.getColor());
        colorCircle.addClickListener(event -> {
            if (group.getAdmins().contains(person)) {
                group.setColor(Group.generateColor());
                groupRepository.save(group);
                colorCircle.setColor(group.getColor());
            }
        });
        summary.add(colorCircle);

        H3 title = new H3();
        title.setText(group.getTitle());
        summary.add(title);

        TextField editTitle = new TextField();
        editTitle.setVisible(false);
        summary.add(editTitle);

        Button editButton = new Button(VaadinIcon.PENCIL.create());
        editButton.setVisible(group.getAdmins().contains(person));
        editButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        summary.add(editButton);

        Button saveEditButton = new Button(VaadinIcon.CHECK.create());
        saveEditButton.setVisible(false);
        saveEditButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        saveEditButton.addClickShortcut(Key.ENTER);
        summary.add(saveEditButton);

        editButton.addClickListener(event -> {
            title.setVisible(false);
            editTitle.setVisible(true);
            editTitle.setValue(group.getTitle());
            editButton.setVisible(false);
            saveEditButton.setVisible(true);
        });

        saveEditButton.addClickListener(event -> {
            if (saveEditButton.isVisible()) {
                if (editTitle.getValue().isBlank()) {
                    editTitle.setInvalid(true);
                    editTitle.setErrorMessage("Bitte gib einen lesbaren Namen ein");
                } else {
                    editTitle.setInvalid(false);
                    editTitle.setErrorMessage("");
                    group.setTitle(editTitle.getValue());
                    groupRepository.save(group);
                    title.setText(editTitle.getValue());
                    title.setVisible(true);
                    editTitle.setVisible(false);
                    editButton.setVisible(true);
                    saveEditButton.setVisible(false);
                }
            }
        });

        boolean admin = group.getAdmins().contains(person);

        HorizontalLayout invite = new HorizontalLayout();
        invite.setAlignItems(Alignment.END);
        invite.setWidthFull();
        invite.setVisible(admin);

        TextField invitationLink = new TextField("Einladungslink");
        invitationLink.setEnabled(false);
        invitationLink.setValue("Temp");
        invite.add(invitationLink);

        Button copyInvitationLink = new Button(VaadinIcon.COPY.create());
        ClipboardHelper clipboardHelper = new ClipboardHelper("Kopieren fehlgeschlagen", copyInvitationLink);
        UI.getCurrent().getPage().fetchCurrentURL(url -> {
            StringBuilder urlBuilder = new StringBuilder();
            urlBuilder.append(url.getProtocol()).append("://");
            urlBuilder.append(url.getHost());
            if(url.getPort() != -1) {
                urlBuilder.append(":").append(url.getPort());
            }
            urlBuilder.append("/group/join/");
            urlBuilder.append(group.getId());
            String joinURL = urlBuilder.toString();
            invitationLink.setValue(joinURL);
            clipboardHelper.setContent(joinURL);
        });
        copyInvitationLink.addClickListener(event -> Notification.show("Einladung in Zwischenablage kopiert"));
        invite.add(clipboardHelper);

        collapsableArea.add(invite);

        Component requestsAndMembers = createPeopleSection(group, person);
        collapsableArea.add(requestsAndMembers);

        HorizontalLayout buttons = new HorizontalLayout();

        Button leave = new Button("Verlassen");
        leave.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        leave.setEnabled(!(group.getAdmins().contains(person) && group.getAdmins().size() == 1) // Nicht der letzte Admin
                && (group.getMembers().size() != 1));// Und nicht die letzte Person => Dann löschen
        leave.addClickListener(event -> {
            ConfirmDialog confirmLeave = new ConfirmDialog(
                    "Bist du sicher, dass du diese Gruppe verlassen möchtest?",
                    "Du musst einen Gruppen Admin bitten dich wieder hinzuzufügen, wenn du das Rückgängig machen möchtest",
                    "Ja, verlassen",
                    e -> {
                        group.getMembers().remove(person);
                        group.getAdmins().remove(person);
                        groupRepository.save(group);
                        grid.setItems(this::updateGrid);
                    }
            );
            confirmLeave.setCancelable(true);
            confirmLeave.setCancelText("Abbruch");
            confirmLeave.open();
        });
        buttons.add(leave);

        Button delete = new Button("Löschen");
        delete.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
        delete.addClickListener(event -> {
            ConfirmDialog confirmDeletion = new ConfirmDialog(
                    "Bist du sicher, dass du diese Gruppe löschen möchtest?",
                    "Alle Termine in dieser Gruppe werden auch gelöscht. Du kannst das nicht Rückgängig machen",
                    "Ja, löschen", e -> {
                dateRepository.findByGroup(group).forEach(date -> {
                    for (Feedback f : date.getFeedbackList()) {
                        feedbackRepository.delete(f);
                    }
                    dateRepository.delete(date);
                });
                groupRepository.delete(group);
                grid.setItems(this::updateGrid);
            });
            confirmDeletion.setCancelable(true);
            confirmDeletion.setCancelText("Abbruch");
            confirmDeletion.open();
        });
        delete.setVisible(admin);
        buttons.add(delete);

        collapsableArea.add(buttons);

        title.addClickListener(event -> collapsableArea.setVisible(!collapsableArea.isVisible()));

        groupWidget.add(summary, collapsableArea);
        return groupWidget;
    }


    private Component createPeopleSection(Group group, Person person) {
        VerticalLayout peopleLayout = new VerticalLayout();
        peopleLayout.setWidthFull();

        HorizontalLayout requests = new HorizontalLayout();
        requests.setWidthFull();
        requests.setAlignItems(Alignment.CENTER);
        H5 requestsLabel = new H5("Anfragen: ");
        requests.add(requestsLabel);
        if (!group.getRequests().isEmpty()) {
            AvatarGroup requestAvatars = new AvatarGroup();
            requestAvatars.addThemeVariants(AvatarGroupVariant.LUMO_SMALL);
            requestAvatars.setItems(getRequestAvatars(group));
            requests.add(requestAvatars);

            requests.addClickListener(event ->
                    createManageRequestsDialog(group, person)
                            .setCloseListener(() -> requestAvatars.setItems(getRequestAvatars(group)))
                            .open());
        } else {
            NativeLabel noRequests = new NativeLabel("Keine Anfragen");
            requests.add(noRequests);
        }
        peopleLayout.add(requests);

        HorizontalLayout members = new HorizontalLayout();
        members.setWidthFull();
        members.setAlignItems(Alignment.CENTER);
        H5 membersLabel = new H5("Mitglieder: ");
        members.add(membersLabel);
        if (!group.getMembers().isEmpty()) {
            AvatarGroup membersAvatars = new AvatarGroup();
            membersAvatars.addThemeVariants(AvatarGroupVariant.LUMO_SMALL);
            membersAvatars.setItems(getMemberAvatars(group));
            members.add(membersAvatars);

            members.addClickListener(event ->
                    createEditMembersDialog(group, person)
                            .setCloseListener(() -> membersAvatars.setItems(getMemberAvatars(group)))
                            .open());
        } else {
            NativeLabel noMembers = new NativeLabel("Keine Mitglieder");
            members.add(noMembers);
        }
        peopleLayout.add(members);

        return peopleLayout;
    }

    private List<AvatarGroup.AvatarGroupItem> getMemberAvatars(Group group) {
        return group.getMembers().stream().map(Person::getAvatarGroupItem).toList();
    }

    private List<AvatarGroup.AvatarGroupItem> getRequestAvatars(Group group) {
        return group.getRequests().stream().map(Person::getAvatarGroupItem).toList();
    }

    private ClosableDialog createManageRequestsDialog(Group group, Person person) {
        ClosableDialog dialog = new ClosableDialog("Beitrittsanfragen");

        Grid<Person> requests = new Grid<>();
        requests.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_NO_ROW_BORDERS);

        requests.addComponentColumn(p -> {
            HorizontalLayout row = new HorizontalLayout();
            row.setAlignItems(Alignment.CENTER);

            Avatar avatar = p.getAvatar();
            row.add(avatar);

            NativeLabel name = new NativeLabel(p.getName());
            row.add(name);

            return row;
        });
        requests.addComponentColumn(p -> {
            HorizontalLayout row = new HorizontalLayout();

            Button confirm = new Button(VaadinIcon.CHECK.create());
            confirm.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SUCCESS);
            confirm.setEnabled(group.getAdmins().contains(person));
            confirm.addClickListener(event -> {
                group.getRequests().remove(p);
                group.getMembers().add(p);
                groupRepository.save(group);
                requests.setItems(group.getRequests());
            });
            row.add(confirm);

            Button remove = new Button(VaadinIcon.CLOSE.create());
            remove.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
            remove.setEnabled(group.getAdmins().contains(person));
            remove.addClickListener(event -> {
                group.getRequests().remove(p);
                groupRepository.save(group);
                requests.setItems(group.getRequests());
            });
            row.add(remove);

            return row;
        });

        requests.setItems(group.getRequests());

        dialog.add(requests);
        dialog.setWidth("500px");

        return dialog;
    }

    private ClosableDialog createEditMembersDialog(Group group, Person person) {
        ClosableDialog dialog = new ClosableDialog("Mitglieder");

        Grid<Person> members = new Grid<>();
        members.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_NO_ROW_BORDERS);

        members.addComponentColumn(p -> {
            Checkbox checkbox = new Checkbox();
            checkbox.setValue(group.getAdmins().contains(p));
            checkbox.setEnabled(group.getAdmins().contains(person) && !person.equals(p));
            checkbox.addValueChangeListener(event -> {
                if (event.getValue()) {
                    if (!group.getAdmins().contains(p)) {
                        group.getAdmins().add(p);
                        groupRepository.save(group);
                    }
                } else {
                    if (group.getAdmins().contains(p)) {
                        group.getAdmins().remove(p);
                        groupRepository.save(group);
                    }
                }
            });
            return checkbox;
        }).setFlexGrow(0).setHeader("Admin");
        members.addComponentColumn(p -> {
            HorizontalLayout row = new HorizontalLayout();
            row.setAlignItems(Alignment.CENTER);

            Avatar avatar = p.getAvatar();
            row.add(avatar);

            NativeLabel name = new NativeLabel(p.getName());
            row.add(name);

            return row;
        }).setHeader("Name");
        members.addComponentColumn(p -> {
            HorizontalLayout row = new HorizontalLayout();

            Button remove = new Button(VaadinIcon.CLOSE.create());
            remove.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
            remove.setEnabled(group.getAdmins().contains(person) && !person.equals(p));
            remove.addClickListener(event -> {
                group.getMembers().remove(p);
                group.getAdmins().remove(p);
                groupRepository.save(group);
                members.setItems(group.getMembers());
            });
            row.add(remove);

            return row;
        }).setFlexGrow(0).setHeader("Entfernen").setTextAlign(ColumnTextAlign.END);

        members.setItems(group.getMembers());

        dialog.add(members);
        dialog.setWidth("500px");

        return dialog;
    }
}