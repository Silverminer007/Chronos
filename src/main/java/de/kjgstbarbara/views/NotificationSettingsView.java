package de.kjgstbarbara.views;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.validator.IntegerRangeValidator;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;
import de.kjgstbarbara.data.Person;
import de.kjgstbarbara.data.Reminder;
import de.kjgstbarbara.service.PersonsRepository;
import de.kjgstbarbara.service.PersonsService;
import de.kjgstbarbara.service.ReminderService;
import de.kjgstbarbara.views.nav.MainNavigationView;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.temporal.ChronoUnit;
import java.util.List;

@Route(value = "notifications", layout = MainNavigationView.class)
@PageTitle("Benachrichtigungen")
@PermitAll
public class NotificationSettingsView extends VerticalLayout {

    public NotificationSettingsView(PersonsService personsService, ReminderService reminderService, AuthenticationContext authenticationContext) {
        PersonsRepository personsRepository = personsService.getPersonsRepository();
        Person person = authenticationContext.getAuthenticatedUser(UserDetails.class)
                .flatMap(userDetails -> personsRepository.findByUsername(userDetails.getUsername()))
                .orElse(null);
        if (person == null) {
            authenticationContext.logout();
        } else {
            Binder<Person> binder = new Binder<>();
            setSizeFull();

            setAlignItems(Alignment.CENTER);
            setJustifyContentMode(JustifyContentMode.CENTER);

            VerticalLayout wrapper = new VerticalLayout();
            wrapper.setWidth("200px");//TODO Find a good way to center on desktop

            Scroller scroller = new Scroller();
            scroller.setHeightFull();
            scroller.setScrollDirection(Scroller.ScrollDirection.VERTICAL);

            VerticalLayout form = new VerticalLayout();
            form.setHeightFull();
            form.setAlignItems(Alignment.START);
            form.setJustifyContentMode(JustifyContentMode.START);

            H3 notifications = new H3("Benachrichtigungen");
            form.add(notifications);

            H3 services = new H3("Dienste");
            form.add(services);

            Checkbox whatsapp = new Checkbox("WhatsApp");
            if (person.getPhoneNumber() == 0) {
                whatsapp.setEnabled(false);
                whatsapp.setTooltipText("Bitte hinterlege zuerst eine Telefonnummer");
            }
            binder.forField(whatsapp).bind(Person::isWhatsappNotifications, Person::setWhatsappNotifications);
            form.add(whatsapp);

            Checkbox email = new Checkbox("E-Mail");
            if (person.getEMailAddress() == null || person.getEMailAddress().isBlank()) {
                email.setEnabled(false);
                email.setTooltipText("Bitte hinterlege zuerst eine E-Mail Adresse");
            }
            binder.forField(email).bind(Person::isEMailNotifications, Person::setEMailNotifications);
            form.add(email);

            H3 time = new H3("Uhrzeit");
            form.add(time);

            IntegerField hour = new IntegerField();
            binder.forField(hour)
                    .withValidator(new IntegerRangeValidator("Es sind nur Werte zwischen 0 und 23 erlaubt", 0, 23))
                    .bind(Person::getRemindMeTime, Person::setRemindMeTime);
            form.add(hour);

            H3 intervals = new H3("Intervalle");
            form.add(intervals);

            Checkbox monthOverview = new Checkbox("Monatsübersicht");
            binder.forField(monthOverview).bind(Person::isMonthOverview, Person::setMonthOverview);
            form.add(monthOverview);

            H3 reminders = new H3("Erinnerungen");
            form.add(reminders);

            HorizontalLayout newReminder = new HorizontalLayout();
            newReminder.setAlignItems(Alignment.END);
            IntegerField amount = new IntegerField("Anzahl");
            newReminder.add(amount);
            Select<ChronoUnit> chronoFieldSelect = new Select<>();
            chronoFieldSelect.setLabel("Tage/Stunden vorher");
            chronoFieldSelect.setItems(List.of(ChronoUnit.HOURS, ChronoUnit.DAYS));
            newReminder.add(chronoFieldSelect);
            Button addReminder = new Button("Add");
            newReminder.add(addReminder);
            form.add(newReminder);

            Grid<Reminder> reminderGrid = new Grid<>(Reminder.class, false);
            reminderGrid.addColumn("amount").setFlexGrow(0).setTextAlign(ColumnTextAlign.END).setHeader("Anzahl").setSortable(false);
            reminderGrid.addColumn(reminder -> {
                if (reminder.getChronoUnit().equals(ChronoUnit.HOURS)) {
                    return reminder.getAmount() == 1 ? "Stunde vorher" : "Stunden vorher";
                } else {
                    return reminder.getAmount() == 1 ? "Tag vorher" : "Tage vorher";
                }
            }).setHeader("Einheit").setSortable(false);
            reminderGrid.addComponentColumn(reminder -> {
                Icon icon = VaadinIcon.CLOSE.create();
                icon.setColor("#ff0000");
                Button delete = new Button(icon);
                delete.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY_INLINE);
                delete.addClickListener(event -> {
                    reminderService.removeReminder(reminder);
                    reminderGrid.setItems(reminderService.getReminders(person));
                });
                return delete;
            }).setFlexGrow(0);
            reminderGrid.setItems(reminderService.getReminders(person));
            form.add(reminderGrid);

            addReminder.addClickListener(event -> {
                if (amount.getValue() == null) {
                    amount.setInvalid(true);
                    amount.setErrorMessage("Keine Anzahl ausgewählt");
                    return;
                } else if (amount.getValue() < 1) {
                    amount.setInvalid(true);
                    amount.setErrorMessage("Du kannst nicht nach einem Termin erinnert werden");
                    return;
                } else {
                    amount.setInvalid(false);
                    amount.setErrorMessage("");
                }
                if (chronoFieldSelect.getValue() == null) {
                    chronoFieldSelect.setInvalid(true);
                    chronoFieldSelect.setErrorMessage("Keine Einheit Ausgewählt");
                    return;
                } else {
                    chronoFieldSelect.setInvalid(false);
                    chronoFieldSelect.setErrorMessage("");
                }
                Reminder reminder = new Reminder();
                reminder.setChronoUnit(chronoFieldSelect.getValue());
                reminder.setAmount(amount.getValue());
                reminder.setPerson(person);
                if (!reminderService.getReminders(person).contains(reminder)) {
                    reminderService.addReminder(reminder);
                    reminderGrid.setItems(reminderService.getReminders(person));
                }

                amount.clear();
                chronoFieldSelect.clear();
            });

            scroller.setContent(form);

            wrapper.add(scroller);

            Button save = new Button("Speichern");
            save.addClickShortcut(Key.ENTER);
            save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            save.setWidthFull();
            save.addClickListener(event -> {
                try {
                    binder.writeBean(person);
                    personsRepository.save(person);
                    Notification.show("Speichern erfolgreich");
                } catch (ValidationException e) {
                    Notification.show(e.getLocalizedMessage());
                }
            });

            wrapper.add(save);

            this.add(wrapper);

            binder.readBean(person);
        }
    }

}