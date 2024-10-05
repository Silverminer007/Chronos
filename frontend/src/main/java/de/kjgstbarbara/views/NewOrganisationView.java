package de.kjgstbarbara.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.*;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.kjgstbarbara.components.*;
import de.kjgstbarbara.data.Group;
import de.kjgstbarbara.data.Organisation;
import de.kjgstbarbara.data.Person;
import de.kjgstbarbara.service.*;
import de.kjgstbarbara.views.security.RegisterView;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.core.userdetails.User;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Route(value = "new-organisations", layout = MainNavigationView.class)
@PageTitle("Organisationen")
@PermitAll
public class NewOrganisationView extends VerticalLayout {
    private final PersonsRepository personsRepository;
    private final OrganisationRepository organisationRepository;
    private final GroupRepository groupRepository;

    private Component organisations = new VerticalLayout();
    private String search = null;

    private final Person person;

    public NewOrganisationView(PersonsService personsService, OrganisationService organisationService,
                                GroupService groupService, AuthenticationContext authenticationContext) {
        this.personsRepository = personsService.getPersonsRepository();
        this.organisationRepository = organisationService.getOrganisationRepository();
        this.groupRepository = groupService.getGroupRepository();
        this.person = authenticationContext.getAuthenticatedUser(User.class)
                .flatMap(userDetails -> personsRepository.findByUsername(userDetails.getUsername()))
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

            organisationHeader.add(organisationInformation);

            Button add = new Button(VaadinIcon.PLUS.create());
            add.addThemeVariants(ButtonVariant.LUMO_CONTRAST, ButtonVariant.LUMO_TERTIARY);
            add.addClickListener(event -> this.addGroup(organisation));
            organisationHeader.add(add);

            organisationHeader.addClickListener(event ->
                    UI.getCurrent().navigate(OrganisationDetailsView.class, new RouteParameters(new RouteParam("organisation", organisation.getId()))));

            organisationLayout.add(organisationHeader);

            List<Group> groups = groupRepository.findByOrganisation(organisation);
            for (Group group : groups) {
                if (!group.getMembers().contains(this.person) && !organisation.getAdmin().equals(this.person)) {
                    continue;
                }
                String search = NewOrganisationView.this.search;
                if (search != null && !organisation.getName().contains(search) && !group.getName().contains(search)) {
                    continue;
                }
                organisationLayout.add(createGroupLayout(group));
            }
            if (groups.isEmpty()) {
                organisationLayout.add(createNoGroupsFoundLabel());
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

        addGroup.getFooter().add(new DialogFooter(addGroup::close, () -> {
            Group group = new Group();
            group.setOrganisation(organisation);
            group.setColor(colorButton.getValue());
            group.setName(nameField.getValue());
            group.getMembers().add(this.person);
            group.getAdmins().add(this.person);
            this.groupRepository.save(group);
            addGroup.close();
            this.createOrganisationAndGroupList();
        }, "Erstellen"));
        addGroup.open();
    }

    private Component createGroupLayout(Group group) {
        HorizontalLayout groupLayout = new HorizontalLayout();
        groupLayout.setWidthFull();
        groupLayout.setAlignItems(Alignment.CENTER);
        groupLayout.setJustifyContentMode(JustifyContentMode.BETWEEN);
        groupLayout.addClassNames(LumoUtility.Background.TINT_5, LumoUtility.BorderRadius.SMALL);
        groupLayout.setPadding(true);

        HorizontalLayout groupInformation = new HorizontalLayout();
        groupInformation.setWidthFull();
        groupInformation.setAlignItems(Alignment.CENTER);
        groupInformation.setJustifyContentMode(JustifyContentMode.START);

        Icon groupColor = VaadinIcon.CIRCLE.create();
        groupColor.setColor(group.getColor());
        groupColor.setSize("10px");
        groupInformation.add(groupColor);

        NativeLabel groupName = new NativeLabel(group.getName());
        groupName.addClassNames(LumoUtility.TextColor.PRIMARY_CONTRAST);
        groupInformation.add(groupName);

        if (group.getAdmins().contains(this.person)) {
            groupInformation.add(createAdminBadge());
        }

        groupLayout.add(groupInformation);

        groupLayout.addClickListener(event ->
                UI.getCurrent().navigate(GroupDetailsView.class, new RouteParameters(new RouteParam("group", group.getId()))));

        groupLayout.add(VaadinIcon.ARROW_RIGHT.create());
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

    private Component createNoGroupsFoundLabel() {
        HorizontalLayout noItemsFoundLabelWrapper = new HorizontalLayout(new H6("Keine Gruppen gefunden"));
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
        joinButton.setEnabled(false);
        footer.add(joinButton);

        Button createButton = new Button("Neu", VaadinIcon.PLUS.create());
        createButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST, ButtonVariant.LUMO_LARGE);
        createButton.addClickListener(event -> addOrganisation());
        footer.add(createButton);

        return footer;
    }

    private void addOrganisation() {
        ClosableDialog addOrganisation = new ClosableDialog("Organisation erstellen");

        HorizontalLayout organisationInformation = new HorizontalLayout();
        organisationInformation.setWidthFull();
        organisationInformation.setAlignItems(Alignment.BASELINE);
        organisationInformation.setJustifyContentMode(JustifyContentMode.START);

        TextField nameField = new TextField("Name");
        nameField.setWidthFull();
        nameField.focus();
        organisationInformation.add(nameField);

        addOrganisation.add(nameField);

        addOrganisation.getFooter().add(new DialogFooter(addOrganisation::close, () -> {
            Organisation organisation = new Organisation();
            organisation.setName(nameField.getValue());
            organisation.getMembers().add(this.person);
            organisation.setAdmin(this.person);
            this.organisationRepository.save(organisation);
            addOrganisation.close();
            this.createOrganisationAndGroupList();
        }, "Erstellen"));
        addOrganisation.open();
    }
}