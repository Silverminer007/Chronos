package de.kjgstbarbara.chronos.views.groups;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.avatar.AvatarVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.*;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.kjgstbarbara.chronos.FrontendUtils;
import de.kjgstbarbara.chronos.components.ClosableDialog;
import de.kjgstbarbara.chronos.components.ColorButton;
import de.kjgstbarbara.chronos.components.Search;
import de.kjgstbarbara.chronos.data.Group;
import de.kjgstbarbara.chronos.data.Person;
import de.kjgstbarbara.chronos.service.*;
import de.kjgstbarbara.chronos.views.MainNavigationView;
import de.kjgstbarbara.chronos.views.RegisterView;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.Comparator;
import java.util.List;

@PermitAll
@Route(value = "organisations/group/:group", layout = MainNavigationView.class)
public class GroupDetailsView extends VerticalLayout implements BeforeEnterObserver {
    private final Person loggedInUser;
    private final GroupRepository groupRepository;
    private final DateRepository dateRepository;
    private String search = null;
    private Component header = new HorizontalLayout();
    private Component members = new VerticalLayout();
    private Component footer = new HorizontalLayout();
    private Group group;

    public GroupDetailsView(PersonsService personsService, GroupService groupService, DatesService datesService, AuthenticationContext authenticationContext) {
        this.groupRepository = groupService.getGroupRepository();
        this.dateRepository = datesService.getDateRepository();
        this.loggedInUser = authenticationContext.getAuthenticatedUser(OidcUser.class)
                .flatMap(userDetails -> personsService.getPersonsRepository().findByUsername(userDetails.getUserInfo().getEmail()))
                .orElse(null);
        if (loggedInUser == null) {
            UI.getCurrent().navigate(RegisterView.class);
        }
        this.setSizeFull();
        this.setAlignItems(Alignment.START);
        this.setJustifyContentMode(JustifyContentMode.START);
        this.setPadding(false);
        this.setSpacing(false);

        this.add(this.header);
        this.add(this.members);
        this.add(this.footer);
    }

    private void createHeader() {
        HorizontalLayout header = new HorizontalLayout();
        header.addClassNames(LumoUtility.Background.PRIMARY);
        header.setWidthFull();
        header.setAlignItems(Alignment.CENTER);
        header.setJustifyContentMode(JustifyContentMode.START);
        header.setPadding(true);
        header.setSpacing(true);

        HorizontalLayout groupInformation = new HorizontalLayout();
        groupInformation.setJustifyContentMode(JustifyContentMode.START);
        groupInformation.setWidthFull();
        groupInformation.setAlignItems(Alignment.CENTER);

        Button back = new Button(VaadinIcon.ARROW_LEFT.create());
        back.addThemeVariants(ButtonVariant.LUMO_CONTRAST, ButtonVariant.LUMO_TERTIARY);
        back.addClickListener(event -> UI.getCurrent().navigate(OrganisationListView.class));
        groupInformation.add(back);

        H4 groupName = new H4(this.group.getName());
        if(this.group.getAdmins().contains(this.loggedInUser) || this.group.getOrganisation().getAdmin().equals(this.loggedInUser)) {
            groupName.addClickListener(e -> this.changeName());
        }
        groupInformation.add(groupName);

        ColorButton colorButton = new ColorButton();
        colorButton.setEnabled(this.group.getAdmins().contains(this.loggedInUser));
        colorButton.setValue(this.group.getColor());
        colorButton.addValueChangeListener(event -> {
            this.group.setColor(event.getValue());
            this.group = this.groupRepository.save(this.group);
        });
        groupInformation.add(colorButton);

        header.add(groupInformation);

        header.add(new Search(searchString -> {
            this.search = searchString;
            this.createMemberList();
        }));

        this.replace(this.header, header);
        this.header = header;
    }

    private void changeName() {
        ClosableDialog changeNameDialog = new ClosableDialog("Namen ändern");

        TextField nameField = new TextField("Name");
        nameField.setValue(this.group.getName());
        changeNameDialog.add(nameField);

        HorizontalLayout footer = new HorizontalLayout();
        footer.setWidthFull();
        footer.setAlignItems(Alignment.CENTER);
        footer.setJustifyContentMode(JustifyContentMode.END);

        Button cancel = new Button("Abbrechen");
        cancel.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        cancel.addClickListener(e -> changeNameDialog.close());
        footer.add(cancel);

        Button save = new Button("Speichern");
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        save.addClickShortcut(Key.ENTER);
        save.addClickListener(e -> {
            this.group.setName(nameField.getValue());
            this.group = this.groupRepository.save(this.group);
            this.createHeader();
            changeNameDialog.close();
        });
        footer.add(save);

        changeNameDialog.getFooter().add(footer);
        changeNameDialog.open();
    }

    private void createMemberList() {
        VerticalLayout membersList = new VerticalLayout();
        membersList.setJustifyContentMode(JustifyContentMode.START);
        membersList.setAlignItems(Alignment.START);
        membersList.setPadding(true);
        membersList.setSpacing(true);
        membersList.setSizeFull();

        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(JustifyContentMode.START);
        header.setAlignItems(Alignment.CENTER);
        header.addClassNames(LumoUtility.Background.PRIMARY_50, LumoUtility.BorderRadius.MEDIUM);

        HorizontalLayout headerLeft = new HorizontalLayout();
        headerLeft.setAlignItems(Alignment.CENTER);
        headerLeft.setJustifyContentMode(JustifyContentMode.START);
        headerLeft.setWidthFull();

        headerLeft.add(new NativeLabel());

        H5 headerTitle = new H5("Mitglieder");
        headerLeft.add(headerTitle);

        header.add(headerLeft);

        Button add = new Button(VaadinIcon.PLUS.create());
        add.addThemeVariants(ButtonVariant.LUMO_CONTRAST, ButtonVariant.LUMO_TERTIARY);
        add.addClickListener(event -> this.addMembers());
        header.add(add);

        membersList.add(header);

        String search = this.search == null ? "" : this.search;
        List<Person> memberPersons = group.getMembers().stream().filter(mp -> mp.getName().contains(search) ||
                (this.group.getAdmins().contains(mp) && "Admin".contains(search))).sorted(Comparator.comparing(Person::getName)).toList();
        for (Person mp : memberPersons) {
            membersList.add(createMemberPersonLayout(mp));
        }
        if (memberPersons.isEmpty()) {
            membersList.add(new H6("Keine Mitglieder gefunden"));
        }


        this.replace(this.members, membersList);
        this.members = membersList;
    }

    private void addMembers() {
        ClosableDialog addMembersDialog = new ClosableDialog("Mitglieder hinzufügen");
        addMembersDialog.setMaxWidth("600px");
        List<Person> possibleNewMembers = this.group.getOrganisation().getMembers().stream().filter(p -> !this.group.getMembers().contains(p)).toList();
        if (possibleNewMembers.isEmpty()) {
            addMembersDialog.add(new H6("Alle Mitglieder von \"" + this.group.getOrganisation().getName() + "\" sind bereits Mitglied dieser Gruppe"));

            HorizontalLayout footer = new HorizontalLayout();
            footer.setAlignItems(Alignment.CENTER);
            footer.setJustifyContentMode(JustifyContentMode.END);
            footer.setWidthFull();

            Button close = new Button("Close", VaadinIcon.CLOSE.create());
            close.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            close.addClickListener(e -> addMembersDialog.close());

            footer.add(close);

            addMembersDialog.getFooter().add(footer);
        } else {
            Grid<Person> newMembersGrid = new Grid<>(Person.class, false);
            newMembersGrid.setItems(possibleNewMembers);
            newMembersGrid.setSelectionMode(Grid.SelectionMode.MULTI);
            newMembersGrid.addThemeVariants(GridVariant.LUMO_NO_BORDER);

            newMembersGrid.addColumn(Person::getName);

            addMembersDialog.add(newMembersGrid);

            HorizontalLayout footer = new HorizontalLayout();
            footer.setWidthFull();
            footer.setJustifyContentMode(JustifyContentMode.BETWEEN);
            footer.setAlignItems(Alignment.CENTER);

            Button cancel = new Button("Abbrechen");
            cancel.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
            cancel.addClickListener(e -> addMembersDialog.close());
            footer.add(cancel);

            Button save = new Button("Hinzufügen");
            save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            save.addClickShortcut(Key.ENTER);
            save.addClickListener(e -> {
                this.group.getMembers().addAll(newMembersGrid.getSelectedItems());
                this.group = this.groupRepository.save(this.group);
                addMembersDialog.close();
                this.createMemberList();
                Notification.show(newMembersGrid.getSelectedItems().size() + " Mitglied(er) hinzugefügt");
            });
            footer.add(save);

            addMembersDialog.getFooter().add(footer);
        }
        addMembersDialog.open();
    }

    private HorizontalLayout createMemberPersonLayout(Person memberPerson) {
        HorizontalLayout memberPersonLayout = new HorizontalLayout();
        memberPersonLayout.setWidthFull();
        memberPersonLayout.setAlignItems(Alignment.CENTER);
        memberPersonLayout.setJustifyContentMode(JustifyContentMode.START);
        memberPersonLayout.addClassNames(LumoUtility.BorderRadius.SMALL, LumoUtility.Background.TINT_5);
        memberPersonLayout.setPadding(true);
        memberPersonLayout.addClassNames(LumoUtility.Padding.SMALL);

        HorizontalLayout memberPersonInformation = new HorizontalLayout();
        memberPersonInformation.setAlignItems(Alignment.CENTER);
        memberPersonInformation.setJustifyContentMode(JustifyContentMode.START);
        memberPersonInformation.setWidthFull();

        memberPersonInformation.add(new NativeLabel());

        Avatar avatarItem = FrontendUtils.getAvatar(memberPerson);
        avatarItem.addThemeVariants(AvatarVariant.LUMO_XSMALL);
        memberPersonInformation.add(avatarItem);

        NativeLabel name = new NativeLabel(memberPerson.getName());
        memberPersonInformation.add(name);

        memberPersonInformation.add(createRoleBadge(memberPerson));

        memberPersonLayout.add(memberPersonInformation);

        if (this.group.getAdmins().contains(this.loggedInUser) || this.group.getOrganisation().getAdmin().equals(this.loggedInUser)) {
            Button remove = new Button(VaadinIcon.CLOSE.create());
            remove.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            remove.addClickListener(event -> {
                this.group.getMembers().remove(memberPerson);
                this.group.getVisible().remove(memberPerson);
                this.group.getAdmins().remove(memberPerson);
                this.group = groupRepository.save(this.group);
                this.createMemberList();
            });
            memberPersonLayout.add(remove);
        }

        return memberPersonLayout;
    }

    private Span createRoleBadge(Person memberPerson) {
        Span badge = new Span(this.group.getAdmins().contains(memberPerson) ? "Admin" : "Mitglied");
        badge.getElement().getThemeList().add("badge small contrast");
        badge.getStyle().set("margin-inline-start", "var(--lumo-space-xs)");
        if (this.group.getAdmins().contains(this.loggedInUser) || this.group.getOrganisation().getAdmin().equals(this.loggedInUser)) {
            badge.addClickListener(e -> this.changeRole(memberPerson));
        }
        return badge;
    }

    private void changeRole(Person memberPerson) {
        ClosableDialog changeRoleDialog = new ClosableDialog(memberPerson.getName());

        RadioButtonGroup<String> role = new RadioButtonGroup<>();
        role.setItems("Admin", "Mitglied");
        role.setValue(this.group.getAdmins().contains(memberPerson) ? "Admin" : "Mitglied");

        changeRoleDialog.add(role);

        HorizontalLayout footer = new HorizontalLayout();
        footer.setWidthFull();
        footer.setAlignItems(Alignment.CENTER);
        footer.setJustifyContentMode(JustifyContentMode.BETWEEN);

        Button cancel = new Button("Abbrechen");
        cancel.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        cancel.addClickListener(e -> changeRoleDialog.close());
        footer.add(cancel);

        Button save = new Button("Speichern");
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        save.addClickShortcut(Key.ENTER);
        save.addClickListener(e -> {
            if (role.getValue().equals("Admin")) {
                this.group.getAdmins().add(memberPerson);
            } else {
                this.group.getAdmins().remove(memberPerson);
            }
            this.group = this.groupRepository.save(this.group);
            this.createHeader();
            this.createMemberList();
            this.createFooter();
            changeRoleDialog.close();
        });
        footer.add(save);

        changeRoleDialog.getFooter().add(footer);
        changeRoleDialog.open();
    }

    private void createFooter() {
        HorizontalLayout footer = new HorizontalLayout();
        footer.setWidthFull();
        footer.setAlignItems(Alignment.CENTER);
        footer.setJustifyContentMode(JustifyContentMode.BETWEEN);
        footer.setSpacing(true);
        footer.setPadding(true);

        Button leave = new Button("Verlassen", VaadinIcon.EXIT.create());
        leave.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        leave.setEnabled(this.group.getMembers().contains(this.loggedInUser));
        leave.addClickListener(event -> {
            this.group.getVisible().remove(this.loggedInUser);
            this.group.getMembers().remove(this.loggedInUser);
            this.group.getAdmins().remove(this.loggedInUser);
            this.groupRepository.save(this.group);
            UI.getCurrent().navigate(OrganisationListView.class);
            Notification.show("Du hast die Gruppe \"" + this.group.getName() + "\" verlassen");
        });
        footer.add(leave);

        Button delete = new Button("Löschen", VaadinIcon.TRASH.create());
        delete.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        delete.setEnabled(this.group.getAdmins().contains(this.loggedInUser) || this.group.getOrganisation().getAdmin().equals(this.loggedInUser));
        delete.addClickListener(event -> {
            ConfirmDialog confirmDeleteDialog = new ConfirmDialog();
            confirmDeleteDialog.setHeader("\"" + this.group.getName() + "\" löschen?");
            long dateCount = dateRepository.countByGroup(this.group);
            confirmDeleteDialog.setText("Dadurch werden auch alle " + dateCount + " Termine der Gruppe unwiderruflich gelöscht");
            confirmDeleteDialog.setConfirmText("Löschen");
            confirmDeleteDialog.setCancelText("Abbrechen");
            confirmDeleteDialog.setCancelable(true);
            confirmDeleteDialog.setCloseOnEsc(true);
            confirmDeleteDialog.addConfirmListener(e -> {
                dateRepository.deleteByGroup(this.group);
                groupRepository.delete(this.group);
                UI.getCurrent().navigate(OrganisationListView.class);
                Notification.show("Die Gruppe \"" + this.group.getName() + "\" und " + dateCount + " Termin(e) wurden gelöscht");
            });
            confirmDeleteDialog.open();
        });
        footer.add(delete);

        this.replace(this.footer, footer);
        this.footer = footer;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        this.group = beforeEnterEvent.getRouteParameters().get("group").map(Long::valueOf).flatMap(groupRepository::findById).orElse(null);
        if (this.group == null ||
                (!this.group.getMembers().contains(this.loggedInUser)
                        && !this.group.getOrganisation().getAdmin().equals(this.loggedInUser))) {
            beforeEnterEvent.rerouteTo(OrganisationListView.class);
            return;
        }
        this.createHeader();
        this.createMemberList();
        this.createFooter();
    }
}