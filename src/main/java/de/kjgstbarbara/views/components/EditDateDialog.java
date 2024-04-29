package de.kjgstbarbara.views.components;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.binder.ValidationResult;
import de.kjgstbarbara.data.Board;
import de.kjgstbarbara.data.Date;
import de.kjgstbarbara.data.Person;
import de.kjgstbarbara.service.BoardsRepository;
import de.kjgstbarbara.service.DateRepository;

public class EditDateDialog extends ClosableDialog {
    public EditDateDialog(Date date, Person person, BoardsRepository boardsRepository, DateRepository dateRepository) {
        Binder<Date> binder = new Binder<>();

        this.setTitle(new H3("Termin erstellen"));


        VerticalLayout content = new VerticalLayout();

        HorizontalLayout firstLine = new HorizontalLayout();
        TextField boardTitle = new TextField("Titel des Termins");
        boardTitle.setRequired(true);
        binder.forField(boardTitle).withValidator(new NonNullValidator<>()).bind(Date::getTitle, Date::setTitle);
        firstLine.add(boardTitle);

        ComboBox<Board> selectBoard = new ComboBox<>();
        selectBoard.setLabel("Board");
        selectBoard.setAllowCustomValue(true);
        selectBoard.addCustomValueSetListener(event -> {
            Board board = new Board();
            board.setTitle(event.getDetail());
            board.getAdmins().add(person);
            board.getMembers().add(person);
            boardsRepository.save(board);
            selectBoard.setItems(boardsRepository.findByAdmin(person));
            selectBoard.setValue(board);
        });
        binder.forField(selectBoard).withValidator(new NonNullValidator<>()).bind(Date::getBoard, Date::setBoard);
        selectBoard.setItems(boardsRepository.findByAdmin(person));
        firstLine.add(selectBoard);

        content.add(firstLine);

        DateTimePicker startPicker = new DateTimePicker("Startzeitpunkt");// Das Datum wird hier als UTC+2 eingegeben und im DateWidget auch als solches angezeigt. Das der Kalender aber die Zeitzone des Besuchers automatisch setzt, sind diese als UTC/0 interpretiert und werden entsprechend verschoben um +2 Stunden
        startPicker.setRequiredIndicatorVisible(true);
        binder.forField(startPicker).withValidator(new NonNullValidator<>()).bind(Date::getStart, Date::setStart);
        content.add(startPicker);

        DateTimePicker endPicker = new DateTimePicker("Endzeitpunkt");
        endPicker.setRequiredIndicatorVisible(true);
        binder.forField(endPicker)
                .withValidator((s, valueContext) ->
                        s.isAfter(startPicker.getValue()) ?
                                ValidationResult.ok() :
                                ValidationResult.error("Das Ende einer Veranstaltung kann nicht vor dessen Beginn liegen"))
                .bind(Date::getEnd, Date::setEnd);
        startPicker.addValueChangeListener(event -> {
            if (endPicker.getValue() == null && event.getValue() != null) {
                endPicker.setValue(event.getValue().plusHours(1));
            }
        });
        content.add(endPicker);

        Checkbox publish = new Checkbox("Publish to Website etc.");
        binder.forField(publish).bind(Date::isPublish, Date::setPublish);
        content.add(publish);

        this.add(content);

        binder.readBean(date);

        HorizontalLayout footer = new HorizontalLayout();
        footer.setWidthFull();
        footer.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        Button save = new Button("Speichern", VaadinIcon.SAFE.create());
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        save.addClickShortcut(Key.ENTER);
        save.addClickListener(event -> {
            try {
                binder.writeBean(date);
                dateRepository.save(date);
                this.close();
            } catch (ValidationException e) {
                Notification.show(e.getLocalizedMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        footer.add(save);
        this.getFooter().add(footer);
    }
}
