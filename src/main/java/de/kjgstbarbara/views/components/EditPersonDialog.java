package de.kjgstbarbara.views.components;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.validator.DateTimeRangeValidator;
import de.kjgstbarbara.data.Board;
import de.kjgstbarbara.data.Date;
import de.kjgstbarbara.data.Person;
import de.kjgstbarbara.service.BoardsRepository;
import de.kjgstbarbara.service.DateRepository;

public class EditPersonDialog extends ConfirmDialog {
    public EditPersonDialog(Date date, Person person, BoardsRepository boardsRepository, DateRepository dateRepository) {
        Binder<Date> binder = new Binder<>();

        this.setCancelable(true);
        this.setCancelText("Abbruch");
        this.setConfirmText("Speichern");

        TextField boardTitle = new TextField("Titel des Termins");
        boardTitle.setRequired(true);
        binder.forField(boardTitle).withValidator(new NonNullValidator<>()).bind(Date::getTitle, Date::setTitle);
        this.add(boardTitle);

        Select<Board> selectBoard = new Select<>();//TODO Aktuelle Auswahl wird nicht angezeigt
        selectBoard.setLabel("Board");
        binder.forField(selectBoard).withValidator(new NonNullValidator<>()).bind(Date::getBoard, Date::setBoard);
        selectBoard.setItems(boardsRepository.findByAdmin(person));
        this.add(selectBoard);

        DateTimePicker startPicker = new DateTimePicker("Startzeitpunkt");
        startPicker.setRequiredIndicatorVisible(true);
        binder.forField(startPicker).withValidator(new NonNullValidator<>()).bind(Date::getStart, Date::setStart);
        this.add(startPicker);

        DateTimePicker endPicker = new DateTimePicker("Endzeitpunkt (optional)");
        binder.forField(endPicker)
                .withValidator(new DateTimeRangeValidator(
                        "Das Enddatum muss nach dem Startdatum liegen",
                        startPicker.getValue(), null))
                .bind(Date::getEnd, Date::setEnd);
        this.add(endPicker);

        Checkbox publish = new Checkbox("Publish to Website etc.");
        binder.forField(publish).bind(Date::isPublish, Date::setPublish);
        this.add(publish);

        binder.readBean(date);

        this.setConfirmButton("Speichern", event -> {
            try {
                binder.writeBean(date);
                dateRepository.save(date);
                this.close();
                UI.getCurrent().getPage().reload();
            } catch (ValidationException e) {
                Notification.show(e.getLocalizedMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
    }
}
