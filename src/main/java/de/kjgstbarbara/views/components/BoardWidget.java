package de.kjgstbarbara.views.components;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import de.kjgstbarbara.data.Board;
import de.kjgstbarbara.data.Person;
import org.vaadin.olli.ClipboardHelper;

public class BoardWidget extends VerticalLayout {
    private final Board board;
    private final Person person;
    private boolean expanded = true;

    public BoardWidget(Board board, Person person) {
        this.board = board;
        this.person = person;
        this.update();
    }

    private void update() {
        this.removeAll();
        HorizontalLayout primaryLine = new HorizontalLayout();
        primaryLine.addClickListener(event -> this.toggle());
        primaryLine.setWidthFull();
        H3 title = new H3();
        primaryLine.add(title);
        title.setText(board.getTitle() + " " + board.getMembers().size() + " Mitglied" + (board.getMembers().size() != 1 ? "er" : ""));
        this.add(primaryLine);
        if (this.expanded) {
            boolean admin = board.getAdmins().contains(person);
            String height = title.getHeight();
            if(admin) {
                TextField invitationLink = new TextField();
                invitationLink.setEnabled(false);
                invitationLink.setValue("http://localhost:8080/boards/join/" + this.board.getId());
                Button copyInvitationLink = new Button(VaadinIcon.COPY.create());
                ClipboardHelper clipboardHelper = new ClipboardHelper("http://localhost:8080/boards/join/" + this.board.getId(), copyInvitationLink);
                copyInvitationLink.addClickListener(event -> Notification.show("Clipboard"));
                invitationLink.setWidth("100%");
                HorizontalLayout invite = new HorizontalLayout(invitationLink, clipboardHelper);
                invite.setWidthFull();
                this.add(invite);
                height += invite.getHeight();
            }
            HorizontalLayout footer = new HorizontalLayout();
            footer.setWidthFull();
            if(admin) {
                Button delete = new Button("Löschen");
                delete.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
                delete.setWidth("50%");
                footer.add(delete);
            }
            Button leave = new Button("Verlassen");
            leave.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_CONTRAST);
            leave.setWidth(admin ? "50%" : "100%");
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
}
