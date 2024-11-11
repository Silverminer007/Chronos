package de.kjgstbarbara.views.organisation;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.kjgstbarbara.Utility;
import de.kjgstbarbara.components.Header;
import de.kjgstbarbara.components.*;
import de.kjgstbarbara.data.Organisation;
import de.kjgstbarbara.data.Person;
import de.kjgstbarbara.messaging.MessageSender;
import de.kjgstbarbara.messaging.Messages;
import de.kjgstbarbara.service.*;
import de.kjgstbarbara.views.MainNavigationView;
import jakarta.annotation.security.PermitAll;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

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
    private Component membersTitle = new HorizontalLayout();
    private Component members = new VerticalLayout();
    private Component footer = new HorizontalLayout();
    private Organisation organisation;

    public OrganisationDetailsView(PersonsService personsService, OrganisationService organisationService, GroupService groupService, DatesService datesService, FeedbackRepository feedbackRepository) {
        this.organisationRepository = organisationService.getOrganisationRepository();
        this.groupRepository = groupService.getGroupRepository();
        this.dateRepository = datesService.getDateRepository();
        this.feedbackRepository = feedbackRepository;
        this.loggedInUser = Utility.getAuthenticatedUser(personsService.getPersonsRepository()).orElse(null);
        if (this.loggedInUser != null) {
            this.init();
        }
    }

    private void init() {
        this.setSizeFull();
        this.setAlignItems(Alignment.START);
        this.setJustifyContentMode(JustifyContentMode.START);
        this.setPadding(false);
        this.setSpacing(false);

        this.add(this.header);
        this.add(this.membersTitle);
        this.add(this.members);
        this.add(this.footer);
    }

    private void createHeader() {
        HorizontalLayout header = new Header();

        HorizontalLayout organisationInformation = new HorizontalLayout();
        organisationInformation.setJustifyContentMode(JustifyContentMode.START);
        organisationInformation.setWidthFull();
        organisationInformation.setAlignItems(Alignment.CENTER);

        Button back = new Button(VaadinIcon.ARROW_LEFT.create());
        back.addThemeVariants(ButtonVariant.LUMO_CONTRAST, ButtonVariant.LUMO_TERTIARY);
        back.addClickListener(event -> UI.getCurrent().navigate(NewOrganisationView.class));
        organisationInformation.add(back);

        H4 groupName = new H4("Organisation: " + this.organisation.getName());
        organisationInformation.add(groupName);

        header.add(organisationInformation);

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

    private void createMembersTitle() {
        HorizontalLayout membersTitle = new HorizontalLayout();
        membersTitle.setWidthFull();
        membersTitle.setJustifyContentMode(JustifyContentMode.START);
        membersTitle.setAlignItems(Alignment.CENTER);
        membersTitle.addClassNames(LumoUtility.Background.PRIMARY_50, LumoUtility.BorderRadius.MEDIUM);

        HorizontalLayout headerLeft = new HorizontalLayout();
        headerLeft.setAlignItems(Alignment.CENTER);
        headerLeft.setJustifyContentMode(JustifyContentMode.START);
        headerLeft.setWidthFull();

        headerLeft.add(new NativeLabel());

        H5 headerTitle = new H5("Mitglieder");
        headerLeft.add(headerTitle);

        membersTitle.add(headerLeft);

        membersTitle.add(new Search(searchString -> {
            this.search = searchString;
            this.createMemberList();
        }));
        this.replace(this.membersTitle, membersTitle);
        this.membersTitle = membersTitle;
    }

    private void createMemberList() {
        VerticalLayout membersList = new VerticalLayout();
        membersList.setJustifyContentMode(JustifyContentMode.START);
        membersList.setAlignItems(Alignment.START);
        membersList.setPadding(true);
        membersList.setSpacing(true);
        membersList.setSizeFull();

        String search = this.search == null ? "" : this.search;
        List<Person> memberPersons = organisation.getMembers().stream().filter(mp -> mp.getName().toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT)) ||
                (this.organisation.getAdmin().equals(mp) && "Admin".contains(search))).sorted(Comparator.comparing(Person::getName)).toList();
        for (Person mp : memberPersons) {
            membersList.add(createMemberPersonLayout(mp));
        }
        if (memberPersons.isEmpty()) {
            membersList.add(new H6("Keine Mitglieder gefunden"));
        }

        Scroller scroller = new Scroller(membersList);
        scroller.setSizeFull();
        this.replace(this.members, scroller);
        this.members = scroller;
    }

    private HorizontalLayout createMemberPersonLayout(Person memberPerson) {
        HorizontalLayout memberPersonLayout = new HorizontalLayout();
        memberPersonLayout.setWidthFull();
        memberPersonLayout.setAlignItems(Alignment.CENTER);
        memberPersonLayout.setJustifyContentMode(JustifyContentMode.START);
        memberPersonLayout.addClassNames(LumoUtility.BorderRadius.SMALL, LumoUtility.Background.TINT_5);
        memberPersonLayout.setPadding(true);
        memberPersonLayout.addClassNames(LumoUtility.Padding.SMALL);

        HorizontalLayout memberPersonInformation = new PersonPPName(memberPerson);

        memberPersonInformation.add(createRoleBadge(memberPerson));

        memberPersonLayout.add(memberPersonInformation);

        if (this.organisation.getAdmin().equals(this.loggedInUser)) {
            Button remove = new Button(VaadinIcon.CLOSE.create());
            remove.setEnabled(!memberPerson.equals(this.loggedInUser));
            remove.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            remove.addClickListener(event -> {
                this.organisation.getMembers().remove(memberPerson);
                this.organisation = organisationRepository.save(this.organisation);
                this.groupRepository.findByOrganisation(this.organisation).forEach(group -> {
                    group.getMembers().remove(memberPerson);
                    this.groupRepository.save(group);
                });
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

        Button inviteNewMember = new Button("Einladen", VaadinIcon.PLUS.create());
        inviteNewMember.setEnabled(this.organisation.getAdmin().equals(this.loggedInUser));
        inviteNewMember.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        inviteNewMember.setEnabled(this.organisation.getAdmin().equals(this.loggedInUser));
        inviteNewMember.addClickListener(event -> this.createInviteDialog());
        footer.add(inviteNewMember);

        Button leave = new Button("Verlassen", VaadinIcon.EXIT.create());
        leave.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
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

        Button delete = new Button("Löschen", VaadinIcon.TRASH.create());
        delete.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
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
        footer.add(this.organisation.getAdmin().equals(this.loggedInUser) ? delete : leave);

        this.replace(this.footer, footer);
        this.footer = footer;
    }

    private void createInviteDialog() {
        ClosableDialog inviteDialog = new ClosableDialog("Mitglieder Einladen");
        inviteDialog.setCloseListener(this::createMemberList);

        H4 inviteLinkTitle = new H4("Einladungslink");
        inviteLinkTitle.addClassNames(LumoUtility.Background.PRIMARY_50, LumoUtility.BorderRadius.MEDIUM);
        inviteDialog.add(inviteLinkTitle);

        NativeLabel inviteLink = new NativeLabel(System.getenv("HOST_DOMAIN") + "/organisation/join/" +
                organisation.getId());
        inviteDialog.add(inviteLink);

        H4 openRequests = new H4("Offene Anfragen");
        openRequests.addClassNames(LumoUtility.Background.PRIMARY_50, LumoUtility.BorderRadius.MEDIUM);
        inviteDialog.add(openRequests);

        VerticalLayout requestsLayout = new VerticalLayout();
        requestsLayout.setSizeFull();
        Scroller requestScroller = new Scroller(requestsLayout);
        requestScroller.setMaxHeight("50%");
        for (Person request : this.organisation.getMembershipRequests()) {
            requestsLayout.add(createProcessOrgMembershipRequestPane(request));
        }
        if (this.organisation.getMembershipRequests().isEmpty()) {
            requestsLayout.add(new H6("Keine offenen Anfragen gefunden"));
        }
        inviteDialog.add(requestScroller);

        inviteDialog.open();
    }

    private Component createProcessOrgMembershipRequestPane(Person member) {
        VerticalLayout processOrgMembershipRequestPane = new VerticalLayout();
        processOrgMembershipRequestPane.addClassNames(LumoUtility.Background.PRIMARY_10, LumoUtility.BorderRadius.MEDIUM);

        processOrgMembershipRequestPane.add(new PersonPPName(member));

        HorizontalLayout buttons = new HorizontalLayout();
        buttons.setJustifyContentMode(JustifyContentMode.EVENLY);

        Button confirmRequest = new Button("Bestätigen", VaadinIcon.THUMBS_UP.create());
        confirmRequest.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        confirmRequest.addClickListener(event -> {
            processOrgMembershipRequestPane.setVisible(false);
            this.organisation.getMembers().add(member);
            this.organisation.getMembershipRequests().remove(member);
            this.organisationRepository.save(this.organisation);
            new MessageSender(member).organisation(organisation).person(member).send(Messages.ORGANISATION_JOIN_REQUEST_ACCEPTED);
        });
        buttons.add(confirmRequest);

        Button declineRequest = new Button("Ablehnen", VaadinIcon.THUMBS_DOWN.create());
        declineRequest.addThemeVariants(ButtonVariant.LUMO_ERROR);
        declineRequest.addClickListener(event -> {
            processOrgMembershipRequestPane.setVisible(false);
            this.organisation.getMembershipRequests().remove(member);
            this.organisationRepository.save(this.organisation);
            new MessageSender(member).organisation(organisation).person(member).send(Messages.ORGANISATION_JOIN_REQUEST_DECLINED);
        });
        buttons.add(declineRequest);

        processOrgMembershipRequestPane.add(buttons);

        return processOrgMembershipRequestPane;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        this.organisation = beforeEnterEvent.getRouteParameters().get("organisation").map(Long::valueOf).flatMap(organisationRepository::findById).orElse(null);
        if (this.organisation == null || !this.organisation.getMembers().contains(this.loggedInUser)) {
            beforeEnterEvent.rerouteTo(NewOrganisationView.class);
            return;
        }
        this.createHeader();
        this.createMembersTitle();
        this.createMemberList();
        this.createFooter();
    }
}