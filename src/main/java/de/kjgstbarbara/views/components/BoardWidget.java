package de.kjgstbarbara.views.components;

import com.vaadin.flow.component.Component;
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
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.theme.lumo.LumoIcon;
import de.kjgstbarbara.data.Board;
import de.kjgstbarbara.data.Feedback;
import de.kjgstbarbara.data.Person;
import de.kjgstbarbara.service.BoardsRepository;
import de.kjgstbarbara.service.DateRepository;
import de.kjgstbarbara.service.FeedbackRepository;
import org.vaadin.olli.ClipboardHelper;

public class BoardWidget extends VerticalLayout {
    private final Board board;
    private final Person person;
    private final BoardsRepository boardsRepository;
    private final DateRepository dateRepository;
    private final FeedbackRepository feedbackRepository;
    private boolean expanded = false;

    public BoardWidget(Board board, Person person, BoardsRepository boardsRepository, DateRepository dateRepository, FeedbackRepository feedbackRepository) {
        this.board = board;
        this.person = person;
        this.boardsRepository = boardsRepository;
        this.dateRepository = dateRepository;
        this.feedbackRepository = feedbackRepository;
        this.update();
    }

    private void update() {
        this.removeAll();
        HorizontalLayout primaryLine = new HorizontalLayout();
        primaryLine.setAlignItems(Alignment.CENTER);
        primaryLine.addClickListener(event -> this.toggle());
        primaryLine.setWidthFull();

        H3 title = new H3();
        title.setText(board.getTitle());
        primaryLine.add(title);

        Button editTitle = new Button(VaadinIcon.PENCIL.create());
        editTitle.setVisible(this.board.getAdmins().contains(this.person));
        editTitle.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        editTitle.addClickListener(event -> editTitleDialog());
        primaryLine.add(editTitle);

        this.add(primaryLine);
        if (this.expanded) {
            boolean admin = board.getAdmins().contains(person);
            String height = title.getHeight();
            if (admin) {
                TextField invitationLink = new TextField("Einladungslink");
                invitationLink.setEnabled(false);
                invitationLink.setValue("Temp");
                Button copyInvitationLink = new Button(VaadinIcon.COPY.create());
                ClipboardHelper clipboardHelper = new ClipboardHelper("", copyInvitationLink);
                UI.getCurrent().getPage().fetchCurrentURL(url -> {
                    String joinURL = "http://" + url.getHost() + ":" + url.getPort() + "/boards/join/" + this.board.getId();
                    invitationLink.setValue(joinURL);
                    clipboardHelper.setContent(joinURL);
                });
                copyInvitationLink.addClickListener(event -> Notification.show("Einladung in Zwischenablage kopiert"));
                invitationLink.setWidth("100%");
                HorizontalLayout invite = new HorizontalLayout(invitationLink, clipboardHelper);
                invite.setAlignItems(Alignment.END);
                invite.setWidthFull();
                this.add(invite);
                height += invite.getHeight();
            }
            this.add(createPeopleSection());
            HorizontalLayout footer = new HorizontalLayout();
            footer.setWidthFull();
            if (admin) {
                Button delete = new Button("Löschen");
                delete.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
                delete.addClickListener(event -> {
                    ConfirmDialog confirmDeletion = new ConfirmDialog(
                            "Bist du sicher, dass du dieses Board löschen möchtest?",
                            "Alle Termine in diesem Board werden auch gelöscht. Du kannst das nicht Rückgängig machen",
                            "Ja, löschen", e -> {
                        dateRepository.findByBoard(this.board).forEach(date -> {
                            for(Feedback f : date.getFeedbackList()) {
                                feedbackRepository.delete(f);
                            }
                            dateRepository.delete(date);
                        });
                        boardsRepository.delete(this.board);
                        UI.getCurrent().getPage().reload();
                    });
                    confirmDeletion.setCancelable(true);
                    confirmDeletion.setCancelText("Abbruch");
                    confirmDeletion.open();
                });
                delete.setWidth("50%");
                footer.add(delete);
            }
            Button leave = new Button("Verlassen");
            leave.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
            leave.setWidth(admin ? "50%" : "100%");
            leave.setEnabled(!(this.board.getAdmins().contains(this.person) && this.board.getAdmins().size() == 1) // Nicht der letzte Admin
                    && (this.board.getMembers().size() != 1));// Und nicht die letzte Person => Dann löschen
            leave.addClickListener(event -> {
                ConfirmDialog confirmLeave = new ConfirmDialog(
                        "Bist du sicher, dass du dieses Board verlassen möchtest?",
                        "Du musst einen Board Administrator bitten dich wieder hinzuzufügen, wenn du das Rückgängig machen möchtest",
                        "Ja, verlassen",
                        e -> {
                            this.board.getMembers().remove(this.person);
                            this.board.getAdmins().remove(this.person);
                            boardsRepository.save(this.board);
                            UI.getCurrent().getPage().reload();
                        }
                );
                confirmLeave.setCancelable(true);
                confirmLeave.setCancelText("Abbruch");
                confirmLeave.open();
            });
            footer.add(leave);
            height += leave.getHeight();

            this.add(footer);

            this.setHeight(height);
        } else {
            this.setHeight(title.getHeight());
        }
    }

    public void toggle() {
        this.expanded = !this.expanded;
        this.update();
    }


    private void editTitleDialog() {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Titel des Boards");
        dialog.setCancelable(true);
        dialog.setCancelText("Zurück");

        TextField textField = new TextField();
        textField.setValue(board.getTitle());
        dialog.add(textField);

        Button save = new Button("Speichern");
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        save.addClickListener(event -> {
            if (textField.getValue().isBlank()) {
                textField.setInvalid(true);
                textField.setErrorMessage("Der Name darf nicht leer sein");
            } else {
                board.setTitle(textField.getValue());
                boardsRepository.save(board);
                dialog.close();
                UI.getCurrent().getPage().reload();
            }
        });
        dialog.setConfirmButton(save);

        dialog.open();
    }

    private Component createPeopleSection() {
        VerticalLayout peopleLayout = new VerticalLayout();
        peopleLayout.setWidthFull();

        HorizontalLayout requests = new HorizontalLayout();
        requests.setWidthFull();
        requests.setAlignItems(Alignment.CENTER);
        H5 requestsLabel = new H5("Anfragen: ");
        requests.add(requestsLabel);
        if (!this.board.getRequests().isEmpty()) {
            requests.addClickListener(event -> createManageRequestsDialog());

            AvatarGroup requestAvatars = new AvatarGroup();
            requestAvatars.addThemeVariants(AvatarGroupVariant.LUMO_SMALL);
            for (Person p : this.board.getRequests()) {
                requestAvatars.add(p.getAvatarGroupItem());
            }
            requests.add(requestAvatars);
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
        if (!this.board.getMembers().isEmpty()) {
            members.addClickListener(event -> createEditMembersDialog());

            AvatarGroup membersAvatars = new AvatarGroup();
            membersAvatars.addThemeVariants(AvatarGroupVariant.LUMO_SMALL);
            for (Person p : this.board.getMembers()) {
                membersAvatars.add(p.getAvatarGroupItem());
            }
            members.add(membersAvatars);
        } else {
            NativeLabel noMembers = new NativeLabel("Keine Mitglieder");
            members.add(noMembers);
        }
        peopleLayout.add(members);

        return peopleLayout;
    }

    private void createManageRequestsDialog() {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Beitrittsanfragen");
        dialog.setCancelable(false);
        dialog.setConfirmButton("Schließen", event -> UI.getCurrent().getPage().reload());

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
            confirm.setEnabled(this.board.getAdmins().contains(this.person));
            confirm.addClickListener(event -> {
                this.board.getRequests().remove(p);
                this.board.getMembers().add(p);
                boardsRepository.save(this.board);
                requests.setItems(this.board.getRequests());
            });
            row.add(confirm);

            Button remove = new Button(VaadinIcon.CLOSE.create());
            remove.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
            remove.setEnabled(this.board.getAdmins().contains(this.person));
            remove.addClickListener(event -> {
                this.board.getRequests().remove(p);
                boardsRepository.save(this.board);
                requests.setItems(this.board.getRequests());
            });
            row.add(remove);

            return row;
        });

        requests.setItems(this.board.getRequests());

        dialog.add(requests);

        dialog.open();
    }

    private void createEditMembersDialog() {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Mitglieder");
        dialog.setCancelable(false);
        dialog.setConfirmButton("Schließen", event -> UI.getCurrent().getPage().reload());

        Grid<Person> members = new Grid<>();
        members.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_NO_ROW_BORDERS);

        members.addComponentColumn(p -> {
            Checkbox checkbox = new Checkbox();
            checkbox.setValue(this.board.getAdmins().contains(p));
            checkbox.setEnabled(this.board.getAdmins().contains(this.person) && !this.person.equals(p));
            checkbox.addValueChangeListener(event -> {
                if (event.getValue()) {
                    if (!this.board.getAdmins().contains(p)) {
                        this.board.getAdmins().add(p);
                        boardsRepository.save(this.board);
                    }
                } else {
                    if (this.board.getAdmins().contains(p)) {
                        this.board.getAdmins().remove(p);
                        boardsRepository.save(this.board);
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
            remove.setEnabled(this.board.getAdmins().contains(this.person) && !this.person.equals(p));
            remove.addClickListener(event -> {
                this.board.getMembers().remove(p);
                this.board.getAdmins().remove(p);
                boardsRepository.save(this.board);
                members.setItems(this.board.getMembers());
            });
            row.add(remove);

            return row;
        }).setFlexGrow(0).setHeader("Entfernen").setTextAlign(ColumnTextAlign.END);

        members.setItems(this.board.getMembers());

        dialog.add(members);

        dialog.open();
    }
}
