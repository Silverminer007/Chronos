package de.kjgstbarbara.chronos.views.groups;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.*;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.kjgstbarbara.chronos.components.*;
import de.kjgstbarbara.chronos.data.Group;
import de.kjgstbarbara.chronos.data.Organisation;
import de.kjgstbarbara.chronos.data.Person;
import de.kjgstbarbara.chronos.service.*;
import de.kjgstbarbara.chronos.views.MainNavigationView;
import de.kjgstbarbara.chronos.views.RegisterView;
import jakarta.annotation.security.PermitAll;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Route(value = "organisations", layout = MainNavigationView.class)
@PageTitle("Organisationen")
@PermitAll
public class OrganisationListView extends VerticalLayout {
    private static final Logger LOGGER = LogManager.getLogger(OrganisationListView.class);
    private final PersonsRepository personsRepository;
    private final OrganisationRepository organisationRepository;
    private final GroupRepository groupRepository;

    private Component organisations = new VerticalLayout();
    private String search = null;

    private final Person person;

    public OrganisationListView(PersonsService personsService, OrganisationService organisationService,
                                GroupService groupService, AuthenticationContext authenticationContext) {
        this.personsRepository = personsService.getPersonsRepository();
        this.organisationRepository = organisationService.getOrganisationRepository();
        this.groupRepository = groupService.getGroupRepository();
        this.person = authenticationContext.getAuthenticatedUser(OidcUser.class)
                .flatMap(userDetails -> personsRepository.findByUsername(userDetails.getUserInfo().getEmail()))
                .orElse(null);
        if (person == null) {
            UI.getCurrent().navigate(RegisterView.class);
        }
        this.setHeightFull();
        this.setPadding(false);
        this.setSpacing(false);

        this.add(createHeader());
        this.add(this.organisations);
        this.createOrganisationAndGroupList();
        this.add(this.createFooter());
    }

    private Stream<Organisation> findOrganisations() {
        if (this.person == null) {
            return Stream.of();
        }
        Set<Organisation> organisations = new HashSet<>(organisationRepository.findByNameIgnoreCaseLikeAndMembersIn(
                "%" + getSearch() + "%",
                List.of(this.person)
        ));
        organisations.addAll(
                groupRepository.findByNameAndPerson(
                        getSearch(),
                        this.person
                ).stream().map(Group::getOrganisation).collect(Collectors.toSet())
        );
        return organisations.stream().sorted(Comparator.comparing(Organisation::getName));
    }

    private String getSearch() {
        return this.search == null ? "" : this.search;
    }

    private Component createHeader() {
        HorizontalLayout header = new HorizontalLayout();
        header.addClassNames(LumoUtility.Background.PRIMARY);
        header.setWidthFull();
        header.setAlignItems(Alignment.CENTER);
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);
        header.setPadding(true);
        header.setSpacing(true);

        header.add(new H4("Gruppen"));

        header.add(new Search(searchString -> {
            this.search = searchString;
            this.createOrganisationAndGroupList();
        }));

        return header;
    }

    private void createOrganisationAndGroupList() {
        VerticalLayout organisationLayout = new VerticalLayout();
        organisationLayout.setSizeFull();
        organisationLayout.setPadding(true);
        organisationLayout.setSpacing(true);
        organisationLayout.setJustifyContentMode(JustifyContentMode.START);
        organisationLayout.setAlignItems(Alignment.START);

        List<Organisation> organisationList = this.findOrganisations().toList();
        if (organisationList.isEmpty()) {
            organisationLayout.add(createNoItemsFoundLabel());
        }
        organisationList.forEach(organisation -> {
            HorizontalLayout organisationHeader = new HorizontalLayout();
            organisationHeader.setWidthFull();
            organisationHeader.setAlignItems(Alignment.CENTER);
            organisationHeader.setJustifyContentMode(JustifyContentMode.START);
            organisationHeader.addClassNames(LumoUtility.BorderRadius.MEDIUM, LumoUtility.Background.PRIMARY_50);

            HorizontalLayout organisationInformation = new HorizontalLayout();
            organisationInformation.setAlignItems(Alignment.BASELINE);
            organisationInformation.setJustifyContentMode(JustifyContentMode.START);
            organisationInformation.setWidthFull();

            organisationInformation.add(new NativeLabel());

            organisationInformation.add(VaadinIcon.OFFICE.create());

            H4 organisationName = new H4(organisation.getName());
            organisationInformation.add(organisationName);

            if (organisation.getAdmin().equals(this.person)) {
                organisationInformation.add(createAdminBadge());
            }

            Button visibleButton = new Button(organisation.getVisible().contains(this.person) ? VaadinIcon.EYE.create() : VaadinIcon.EYE_SLASH.create());
            visibleButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST, ButtonVariant.LUMO_TERTIARY);
            visibleButton.addClickListener(event -> {
                Organisation o = organisationRepository.findById(organisation.getId()).orElse(organisation);
                if (o.getVisible().contains(this.person)) {
                    o.getVisible().remove(this.person);
                    Notification.show("Termine der Organisation \"" + o.getName() + "\" werden ausgeblendet");
                } else {
                    o.getVisible().add(this.person);
                    Notification.show("Termine der Organisation \"" + o.getName() + "\" werden wieder angezeigt");
                }
                organisationRepository.save(o);
                this.createOrganisationAndGroupList();
            });
            organisationInformation.add(visibleButton);

            organisationHeader.add(organisationInformation);

            Button add = new Button(VaadinIcon.PLUS.create());
            add.addThemeVariants(ButtonVariant.LUMO_CONTRAST, ButtonVariant.LUMO_TERTIARY);
            add.addClickListener(event -> this.addGroup(organisation));
            organisationHeader.add(add);

            organisationLayout.add(organisationHeader);

            List<Group> groups = groupRepository.findByOrganisation(organisation);
            for (Group group : groups) {
                if (!group.getMembers().contains(this.person) && !organisation.getAdmin().equals(this.person)) {
                    continue;
                }
                String search = OrganisationListView.this.search;
                if (search != null && !organisation.getName().contains(search) && !group.getName().contains(search)) {
                    continue;
                }
                organisationLayout.add(createGroupLayout(group));
            }
            if (groups.isEmpty()) {
                organisationLayout.add(createNoItemsFoundLabel());
            }
        });

        this.replace(this.organisations, organisationLayout);
        this.organisations = organisationLayout;
    }

    private void addGroup(Organisation organisation) {
        ClosableDialog addGroup = new ClosableDialog("Gruppe erstellen");

        addGroup.add(new H6("Gehört zu: " + organisation.getName()));

        HorizontalLayout groupInformation = new HorizontalLayout();
        groupInformation.setWidthFull();
        groupInformation.setAlignItems(Alignment.BASELINE);
        groupInformation.setJustifyContentMode(JustifyContentMode.START);

        ColorButton colorButton = new ColorButton();
        colorButton.setValue(Group.generateColor());
        groupInformation.add(colorButton);

        TextField nameField = new TextField("Name");
        nameField.focus();
        groupInformation.add(nameField);

        addGroup.add(nameField);

        HorizontalLayout footer = new HorizontalLayout();
        footer.setWidthFull();
        footer.setAlignItems(Alignment.CENTER);
        footer.setJustifyContentMode(JustifyContentMode.BETWEEN);

        Button cancel = new Button("Abbrechen");
        cancel.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        cancel.addClickListener(e -> addGroup.close());
        footer.add(cancel);

        Button create = new Button("Erstellen");
        create.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        create.addClickShortcut(Key.ENTER);
        create.addClickListener(e -> {
            Group group = new Group();
            group.setOrganisation(organisation);
            group.setColor(colorButton.getValue());
            group.setName(nameField.getValue());
            group.getMembers().add(this.person);
            group.getAdmins().add(this.person);
            group.getVisible().add(this.person);
            this.groupRepository.save(group);
            addGroup.close();
            this.createOrganisationAndGroupList();
        });
        footer.add(create);

        addGroup.getFooter().add(footer);
        addGroup.open();
    }

    private Component createGroupLayout(Group group) {
        HorizontalLayout groupLayout = new HorizontalLayout();
        groupLayout.setWidthFull();
        groupLayout.setAlignItems(Alignment.CENTER);
        groupLayout.setJustifyContentMode(JustifyContentMode.START);
        groupLayout.addClassNames(LumoUtility.Background.TINT_5, LumoUtility.BorderRadius.SMALL);

        HorizontalLayout groupInformation = new HorizontalLayout();
        groupInformation.setWidthFull();
        groupInformation.setAlignItems(Alignment.CENTER);
        groupInformation.setJustifyContentMode(JustifyContentMode.START);

        groupInformation.add(new NativeLabel());

        Icon groupColor = VaadinIcon.CIRCLE.create();
        groupColor.setColor(group.getColor());
        groupColor.setSize("10px");
        groupInformation.add(groupColor);

        NativeLabel groupName = new NativeLabel(group.getName());
        groupName.addClassNames(group.getVisible().contains(this.person) ? LumoUtility.TextColor.PRIMARY_CONTRAST : LumoUtility.TextColor.DISABLED);
        groupName.addClassNames(group.getVisible().contains(this.person) && group.getOrganisation().getVisible().contains(this.person)
                ? LumoUtility.TextColor.PRIMARY_CONTRAST : LumoUtility.TextColor.DISABLED);
        groupName.removeClassName(!(group.getVisible().contains(this.person) && group.getOrganisation().getVisible().contains(this.person))
                ? LumoUtility.TextColor.PRIMARY_CONTRAST : LumoUtility.TextColor.DISABLED);
        groupInformation.add(groupName);

        if (group.getAdmins().contains(this.person)) {
            groupInformation.add(createAdminBadge());
        }

        groupInformation.addClickListener(event ->
                UI.getCurrent().navigate(GroupDetailsView.class, new RouteParameters(new RouteParam("group", group.getId()))));

        groupLayout.add(groupInformation);

        Button visibleButton = new Button(group.getVisible().contains(this.person) ? VaadinIcon.EYE.create() : VaadinIcon.EYE_SLASH.create());
        visibleButton.setEnabled(group.getOrganisation().getVisible().contains(this.person));
        visibleButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST, ButtonVariant.LUMO_TERTIARY);
        visibleButton.addClickListener(event -> {
            Group g = groupRepository.findById(group.getId()).orElse(group);
            if (g.getVisible().contains(this.person)) {
                g.getVisible().remove(this.person);
                Notification.show("Termine der Gruppe \"" + g.getName() + "\" werden ausgeblendet");
            } else {
                g.getVisible().add(this.person);
                Notification.show("Termine der Gruppe \"" + g.getName() + "\" werden wieder angezeigt");
            }
            groupRepository.save(g);
            visibleButton.setIcon(g.getVisible().contains(this.person) ? VaadinIcon.EYE.create() : VaadinIcon.EYE_SLASH.create());
            groupName.removeClassName(!(g.getVisible().contains(this.person) && group.getOrganisation().getVisible().contains(this.person))
                    ? LumoUtility.TextColor.PRIMARY_CONTRAST : LumoUtility.TextColor.DISABLED);
            groupName.addClassNames(g.getVisible().contains(this.person) && group.getOrganisation().getVisible().contains(this.person)
                    ? LumoUtility.TextColor.PRIMARY_CONTRAST : LumoUtility.TextColor.DISABLED);
        });
        groupLayout.add(visibleButton);
        return groupLayout;
    }

    private Span createAdminBadge() {
        Span badge = new Span("Admin");
        badge.getElement().getThemeList().add("badge small contrast");
        badge.getStyle().set("margin-inline-start", "var(--lumo-space-xs)");
        return badge;
    }

    private Component createNoItemsFoundLabel() {
        HorizontalLayout noItemsFoundLabelWrapper = new HorizontalLayout(new H6("Nichts zu sehen"));
        noItemsFoundLabelWrapper.setPadding(true);
        noItemsFoundLabelWrapper.setSpacing(false);
        return noItemsFoundLabelWrapper;
    }

    private Component createFooter() {
        HorizontalLayout footer = new HorizontalLayout();
        footer.setSpacing(true);
        footer.setPadding(true);
        footer.addClassNames(LumoUtility.Width.FULL,
                LumoUtility.JustifyContent.BETWEEN,
                LumoUtility.AlignSelf.STRETCH);
        footer.setAlignItems(Alignment.CENTER);

        Button joinButton = new Button("Beitreten");
        joinButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST, ButtonVariant.LUMO_LARGE);
        joinButton.addClickListener(event -> {
        });
        footer.add(joinButton);

        Button createButton = new Button("Neu", VaadinIcon.PLUS.create());
        createButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST, ButtonVariant.LUMO_LARGE);
        //createButton.addClickListener(event -> createCreateDialog().open());
        footer.add(createButton);

        return footer;
    }
}