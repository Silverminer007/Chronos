package de.kjgstbarbara.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.avatar.AvatarVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.kjgstbarbara.FrontendUtils;
import de.kjgstbarbara.Utility;
import de.kjgstbarbara.components.ClosableDialog;
import de.kjgstbarbara.components.DialogFooter;
import de.kjgstbarbara.components.Header;
import de.kjgstbarbara.components.Search;
import de.kjgstbarbara.data.Organisation;
import de.kjgstbarbara.data.Person;
import de.kjgstbarbara.service.*;
import de.kjgstbarbara.views.security.RegisterView;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.core.userdetails.User;
import org.vaadin.olli.ClipboardHelper;

import java.util.Comparator;
import java.util.List;

@PermitAll
@Route(value = "new-organisations/organisation/:organisation", layout = MainNavigationView.class)
public class OrganisationDetailsView extends VerticalLayout implements BeforeEnterObserver {
    private final Person loggedInUser;
    private final OrganisationRepository organisationRepository;
    private final GroupRepository groupRepository;
    private final DateRepository dateRepository;
    private final FeedbackRepository feedbackRepository;
    private String search = null;
    private Component header = new HorizontalLayout();
    private Component invite = new HorizontalLayout();
    private Component members = new VerticalLayout();
    private Component footer = new HorizontalLayout();
    private Organisation organisation;

    public OrganisationDetailsView(PersonsService personsService, OrganisationService organisationService, GroupService groupService, DatesService datesService, AuthenticationContext authenticationContext, FeedbackRepository feedbackRepository) {
        this.organisationRepository = organisationService.getOrganisationRepository();
        this.groupRepository = groupService.getGroupRepository();
        this.dateRepository = datesService.getDateRepository();
        this.loggedInUser = authenticationContext.getAuthenticatedUser(User.class)
                .flatMap(userDetails -> personsService.getPersonsRepository().findByUsername(userDetails.getUsername()))
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
        this.add(this.invite);
        this.add(this.members);
        this.add(this.footer);
        this.feedbackRepository = feedbackRepository;
    }

    private void createHeader() {
        HorizontalLayout header = new Header();

        HorizontalLayout groupInformation = new HorizontalLayout();
        groupInformation.setJustifyContentMode(JustifyContentMode.START);
        groupInformation.setWidthFull();
        groupInformation.setAlignItems(Alignment.CENTER);

        Button back = new Button(VaadinIcon.ARROW_LEFT.create());
        back.addThemeVariants(ButtonVariant.LUMO_CONTRAST, ButtonVariant.LUMO_TERTIARY);
        back.addClickListener(event -> UI.getCurrent().navigate(NewOrganisationView.class));
        groupInformation.add(back);

        H4 groupName = new H4("Organisation: " + this.organisation.getName());
        groupInformation.add(groupName);

        header.add(groupInformation);

        Button editName = new Button(VaadinIcon.PENCIL.create());
        editName.addThemeVariants(ButtonVariant.LUMO_CONTRAST, ButtonVariant.LUMO_TERTIARY_INLINE);
        editName.setEnabled(this.organisation.getAdmin().equals(this.loggedInUser));
        editName.addClickListener(event -> this.changeName());
        header.add(editName);

        this.replace(this.header, header);
        this.header = header;
    }

    private void changeName() {
        ClosableDialog changeNameDialog = new ClosableDialog("Namen ändern");

        TextField nameField = new TextField("Name");
        nameField.setValue(this.organisation.getName());
        changeNameDialog.add(nameField);

        changeNameDialog.getFooter().add(new DialogFooter(changeNameDialog::close, () -> {
            this.organisation.setName(nameField.getValue());
            this.organisation = this.organisationRepository.save(this.organisation);
            this.createHeader();
            changeNameDialog.close();
        }, "Speichern"));
        changeNameDialog.open();
    }

    private void createInviteLink() {
        HorizontalLayout invite = new HorizontalLayout();
        invite.setAlignItems(Alignment.END);
        invite.setWidthFull();
        invite.setPadding(true);
        invite.setVisible(this.organisation.getAdmin().equals(this.loggedInUser));

        TextField invitationLink = new TextField("Einladungslink");
        invitationLink.setEnabled(false);// Causes "Ignoring update for disabled return channel", but can be ignored, as it's not intended that something should change here, when the user expands the widget
        invitationLink.setValue("Temp");
        invite.add(invitationLink);

        Button copyInvitationLink = new Button(VaadinIcon.COPY.create());
        ClipboardHelper clipboardHelper = new ClipboardHelper("Kopieren fehlgeschlagen", copyInvitationLink);
        UI.getCurrent().getPage().fetchCurrentURL(url -> {
            String joinURL = Utility.baseURL(url) + "/organisation/join/" +
                    organisation.getId();
            invitationLink.setValue(joinURL);
            clipboardHelper.setContent(joinURL);
        });
        copyInvitationLink.addClickListener(event -> Notification.show("Einladung in Zwischenablage kopiert"));
        invite.add(clipboardHelper);
        invite.add(copyInvitationLink);

        this.replace(this.invite, invite);
        this.invite = invite;
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

        header.add(new Search(searchString -> {
            this.search = searchString;
            this.createMemberList();
        }));

        membersList.add(header);

        String search = this.search == null ? "" : this.search;
        List<Person> memberPersons = organisation.getMembers().stream().filter(mp -> mp.getName().contains(search) ||
                (this.organisation.getAdmin().equals(mp) && "Admin".contains(search))).sorted(Comparator.comparing(Person::getName)).toList();
        for (Person mp : memberPersons) {
            membersList.add(createMemberPersonLayout(mp));
        }
        if (memberPersons.isEmpty()) {
            membersList.add(new H6("Keine Mitglieder gefunden"));
        }


        this.replace(this.members, membersList);
        this.members = membersList;
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

        if (this.organisation.getAdmin().equals(this.loggedInUser)) {
            Button remove = new Button(VaadinIcon.CLOSE.create());
            remove.setEnabled(!memberPerson.equals(this.loggedInUser));
            remove.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            remove.addClickListener(event -> {
                this.organisation.getMembers().remove(memberPerson);
                this.organisation = organisationRepository.save(this.organisation);
                this.createMemberList();
            });
            memberPersonLayout.add(remove);
        }

        return memberPersonLayout;
    }

    private Span createRoleBadge(Person memberPerson) {
        Span badge = new Span(this.organisation.getAdmin().equals(memberPerson) ? "Admin" : "Mitglied");
        badge.getElement().getThemeList().add("badge small contrast");
        badge.getStyle().set("margin-inline-start", "var(--lumo-space-xs)");
        if (this.organisation.getAdmin().equals(this.loggedInUser)) {
            badge.addClickListener(e -> this.changeRole(memberPerson));
        }
        return badge;
    }

    private void changeRole(Person memberPerson) {
        ClosableDialog changeRoleDialog = new ClosableDialog(memberPerson.getName());

        RadioButtonGroup<String> role = new RadioButtonGroup<>();
        role.setItems("Admin", "Mitglied");
        role.setValue(this.organisation.getAdmin().equals(memberPerson) ? "Admin" : "Mitglied");

        changeRoleDialog.add(role);

        changeRoleDialog.getFooter().add(new DialogFooter(changeRoleDialog::close, () -> {
            if (role.getValue().equals("Admin")) {
                this.organisation.setAdmin(memberPerson);
            }
            this.organisation = this.organisationRepository.save(this.organisation);
            this.createHeader();
            this.createMemberList();
            this.createFooter();
            changeRoleDialog.close();
        }, "Speichern"));
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
        leave.setEnabled(this.organisation.getMembers().contains(this.loggedInUser) && !this.organisation.getAdmin().equals(this.loggedInUser));
        leave.addClickListener(event -> {
            this.groupRepository.findByOrganisation(this.organisation).forEach(group -> {
                group.getMembers().remove(this.loggedInUser);
                group.getAdmins().remove(this.loggedInUser);
                this.groupRepository.save(group);
            });
            this.organisation.getMembers().remove(this.loggedInUser);
            this.organisationRepository.save(this.organisation);
            UI.getCurrent().navigate(NewOrganisationView.class);
            Notification.show("Du hast die Organisation \"" + this.organisation.getName() + "\" verlassen");
        });
        footer.add(leave);

        Button delete = new Button("Löschen", VaadinIcon.TRASH.create());
        delete.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        delete.setEnabled(this.organisation.getAdmin().equals(this.loggedInUser));
        delete.addClickListener(event -> {
            ConfirmDialog confirmDeleteDialog = new ConfirmDialog();
            confirmDeleteDialog.setHeader("\"" + this.organisation.getName() + "\" löschen?");
            long dateCount = dateRepository.countByGroupOrganisation(this.organisation);
            long groupCount = this.groupRepository.countByOrganisation(this.organisation);
            confirmDeleteDialog.setText("Dadurch werden auch alle " + dateCount + " Termine und alle " + groupCount + " Gruppen der Organisation unwiderruflich gelöscht");
            confirmDeleteDialog.setConfirmText("Löschen");
            confirmDeleteDialog.setCancelText("Abbrechen");
            confirmDeleteDialog.setCancelable(true);
            confirmDeleteDialog.setCloseOnEsc(true);
            confirmDeleteDialog.addConfirmListener(e -> {
                this.groupRepository.findByOrganisation(this.organisation).forEach(group -> {
                    dateRepository.findByGroup(group).forEach(date -> {
                        feedbackRepository.deleteAll(date.getFeedbackList());
                        date.getFeedbackList().clear();
                        dateRepository.save(date);
                    });
                    groupRepository.delete(group);
                });
                this.organisationRepository.delete(this.organisation);
                UI.getCurrent().navigate(NewOrganisationView.class);
                Notification.show("Die Organisation \"" + this.organisation.getName() + "\", " + groupCount + " Gruppe(n) und " + dateCount + " Termin(e) wurden gelöscht");
            });
            confirmDeleteDialog.open();
        });
        footer.add(delete);

        this.replace(this.footer, footer);
        this.footer = footer;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        this.organisation = beforeEnterEvent.getRouteParameters().get("organisation").map(Long::valueOf).flatMap(organisationRepository::findById).orElse(null);
        if (this.organisation == null || !this.organisation.getMembers().contains(this.loggedInUser)) {
            beforeEnterEvent.rerouteTo(NewOrganisationView.class);
            return;
        }
        this.createHeader();
        this.createInviteLink();
        this.createMemberList();
        this.createFooter();
    }
}