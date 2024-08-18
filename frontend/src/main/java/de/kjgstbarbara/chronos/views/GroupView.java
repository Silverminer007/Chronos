package de.kjgstbarbara.chronos.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.avatar.AvatarGroup;
import com.vaadin.flow.component.avatar.AvatarGroupVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H5;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;
import de.kjgstbarbara.chronos.FrontendUtils;
import de.kjgstbarbara.chronos.data.Feedback;
import de.kjgstbarbara.chronos.data.Group;
import de.kjgstbarbara.chronos.data.Organisation;
import de.kjgstbarbara.chronos.data.Person;
import de.kjgstbarbara.chronos.service.*;
import de.kjgstbarbara.chronos.components.ClosableDialog;
import jakarta.annotation.security.PermitAll;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Route(value = "groups", layout = MainNavigationView.class)
@PageTitle("Gruppen")
@PermitAll
public class GroupView extends VerticalLayout {
    private final PersonsRepository personsRepository;
    private final OrganisationRepository organisationRepository;
    private final GroupRepository groupRepository;
    private final DateRepository dateRepository;
    private final FeedbackRepository feedbackRepository;

    private final Grid<Group> grid = new Grid<>(Group.class, false);
    private final TextField search = new TextField();

    private final Person person;

    public GroupView(PersonsService personsService, OrganisationService organisationService, GroupService groupService, DatesService datesService, FeedbackService feedbackService, AuthenticationContext authenticationContext) {
        this.personsRepository = personsService.getPersonsRepository();
        this.organisationRepository = organisationService.getOrganisationRepository();
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
            FormLayout formLayout = new FormLayout();
            TextField name = new TextField("Name der Gruppe");
            name.focus();
            formLayout.add(name);
            ComboBox<Organisation> organisationComboBox = new ComboBox<>("Organisation");
            organisationComboBox.setItems(organisationRepository.findByMembersIn(this.person));
            formLayout.add(organisationComboBox);
            Button create = new Button("Erstellen");
            create.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            create.addClickShortcut(Key.ENTER);
            create.addClickListener(e -> {
                if (name.getValue().isBlank()) {
                    name.setInvalid(true);
                    name.setErrorMessage("Dieses Feld ist erforderlich");
                } else if (organisationComboBox.getValue() == null) {
                    name.setInvalid(false);
                    name.setErrorMessage("");
                    organisationComboBox.setInvalid(true);
                    organisationComboBox.setErrorMessage("Bitte wähle eine Organisation");
                } else {
                    Group newGroup = new Group();
                    newGroup.setName(name.getValue());
                    newGroup.setOrganisation(organisationComboBox.getValue());
                    newGroup.getMembers().add(person);
                    newGroup.getAdmins().add(person);
                    groupRepository.save(newGroup);
                    grid.setItems(this::updateGrid);
                    createNewGroupDialog.close();
                }
            });
            formLayout.setResponsiveSteps(
                    new FormLayout.ResponsiveStep("0", 1),
                    new FormLayout.ResponsiveStep("500px", 2)
            );
            createNewGroupDialog.add(formLayout);
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
        return groupRepository.findByNameIgnoreCaseLikeAndMembersIn(
                "%" + search.getValue() + "%",
                List.of(this.person),
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
        title.setText(group.getName());
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
            editTitle.setValue(group.getName());
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
                    group.setName(editTitle.getValue());
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

        Component members = createPeopleSection(group, person);
        collapsableArea.add(members);

        HorizontalLayout buttons = new HorizontalLayout();

        Button leave = new Button("Verlassen");
        leave.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        leave.setEnabled(!(group.getAdmins().contains(person) && group.getAdmins().size() == 1) // Nicht der letzte Admin
                && (group.getMembers().size() != 1));// Und nicht die letzte Person → Dann löschen
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
        return group.getMembers().stream().map(FrontendUtils::getAvatarGroupItem).toList();
    }

    private ClosableDialog createEditMembersDialog(Group group, Person person) {
        List<Person> addablePersons = new ArrayList<>(group.getOrganisation().getMembers());
        MultiSelectComboBox<Person> addPersonSelector = new MultiSelectComboBox<>();

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

            Avatar avatar = FrontendUtils.getAvatar(p);
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
                addablePersons.add(p);
                addPersonSelector.setItems(addablePersons);
                groupRepository.save(group);
                members.setItems(group.getMembers());
            });
            row.add(remove);

            return row;
        }).setFlexGrow(0).setHeader("Entfernen").setTextAlign(ColumnTextAlign.END);

        members.setItems(group.getMembers());

        dialog.add(members);
        dialog.setWidth("500px");

        HorizontalLayout addPerson = new HorizontalLayout();

        addablePersons.removeAll(group.getMembers());
        addPersonSelector.setItems(addablePersons);
        addPersonSelector.setItemLabelGenerator(Person::getName);
        addPerson.add(addPersonSelector);

        Button addPersonButton = new Button("Mitglied hinzufügen");
        addPersonButton.addClickListener(event -> {
            if(addPersonSelector.getValue() != null && !addPersonSelector.getValue().isEmpty()) {
                group.getMembers().addAll(addPersonSelector.getValue());
                members.setItems(group.getMembers());
                addablePersons.removeAll(addPersonSelector.getValue());
                addPersonSelector.setItems(addablePersons);
                groupRepository.save(group);
                addPersonSelector.clear();
            }
        });
        addPerson.add(addPersonButton);

        dialog.add(addPerson);

        return dialog;
    }
}