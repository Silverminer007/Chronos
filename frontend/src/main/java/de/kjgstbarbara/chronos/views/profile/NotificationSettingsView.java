package de.kjgstbarbara.chronos.views.profile;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;
import de.kjgstbarbara.chronos.data.Person;
import de.kjgstbarbara.chronos.service.PersonsRepository;
import de.kjgstbarbara.chronos.service.PersonsService;
import de.kjgstbarbara.chronos.views.MainNavigationView;
import jakarta.annotation.security.PermitAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.vaadin.addons.taefi.component.ToggleButtonGroup;

@Route(value = "notifications", layout = MainNavigationView.class)
@PageTitle("Benachrichtigungen")
@PermitAll
public class NotificationSettingsView extends VerticalLayout {
    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationSettingsView.class);

    public NotificationSettingsView(PersonsService personsService, AuthenticationContext authenticationContext) {
        PersonsRepository personsRepository = personsService.getPersonsRepository();
        Person person = authenticationContext.getAuthenticatedUser(UserDetails.class)
                .flatMap(userDetails -> personsRepository.findByUsername(userDetails.getUsername()))
                .orElse(null);
        if (person == null) {
            authenticationContext.logout();
        } else {
            setSizeFull();

            Binder<Person> binder = new Binder<>();

            H2 title = new H2("Terminerinnerungen");
            this.add(title);
            NativeLabel introduction = new NativeLabel("Du kannst über das KjG Termintool Terminerinnerungen erhalten. Hier kannst du auswählen wann, wie oft und worüber du deine Benachrichtigungen erhalten möchtest");
            this.add(introduction);

            H3 tool = new H3("Worüber möchtest Benachrichtigungen erhalten?");
            this.add(tool);
            NativeLabel toolExplanation = new NativeLabel("Du bekommst die Nachrichten dann an die E-Mail Adresse bzw Telefonnummer die du in deinem Profil hinterlegt hast");
            this.add(toolExplanation);
            ToggleButtonGroup<Person.Reminder> toolSelection = new ToggleButtonGroup<>();
            toolSelection.setItems(Person.Reminder.values());
            toolSelection.setItemLabelGenerator(Person.Reminder::getText);
            toolSelection.setOrientation(ToggleButtonGroup.Orientation.VERTICAL);
            this.add(toolSelection);
            binder.forField(toolSelection).bind(Person::getReminder, Person::setReminder);

            H3 when = new H3("Wann möchtest du Erinnerungen bekommen?");
            this.add(when);

            H4 monthOverviewTitle = new H4("Monatsübersicht");
            this.add(monthOverviewTitle);
            NativeLabel monthOverviewExplanation = new NativeLabel("Am 01. jeden Monats kannst du eine Übersicht deiner Termine bei der KjG erhalten, die im kommenden Monat anstehen");
            this.add(monthOverviewExplanation);
            Checkbox monthOverview = new Checkbox("Monatsübersicht");
            this.add(monthOverview);
            binder.forField(monthOverview).bind(Person::isMonthOverview, Person::setMonthOverview);

            H4 remindHour = new H4("Erinnerungszeit");
            this.add(remindHour);
            NativeLabel reminderHoursExplanation = new NativeLabel(
                    "Wenn du eine Erinnerung für eine Veranstaltung in einem nicht Stundengenauen Abstand haben möchtest, kannst du hier einstellen um wie viel Uhr du deine Terminerinnerungen gebündelt bekommen möchtest. Wenn du hier 19 Uhr einstellen würdest und 2 Tage vorher eine Terminerinnerung bekommen möchtest würdest du für einen Termin am Montag Freitag Abend um 19 Uhr einer Erinnerung bekommen, egal ob der Termin Montags um 9 Uhr morgens oder 17 Uhr Nachmittags ist");
            this.add(reminderHoursExplanation);
            MultiSelectComboBox<Integer> remindHours = new MultiSelectComboBox<>();// TODO Wieso schlägt das speichern bei vielen Elementen fehl?
            remindHours.setItems(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23);
            remindHours.setItemLabelGenerator(i -> i + " Uhr");
            this.add(remindHours);
            binder.forField(remindHours).bind(Person::getRemindMeTime, Person::setRemindMeTime);

            H4 remindIntervalsTitle = new H4("Erinnerungen");
            this.add(remindIntervalsTitle);
            MultiSelectComboBox<Integer> dayReminderInterval = new MultiSelectComboBox<>("Tägliche Erinnerungen");
            dayReminderInterval.setItems(0, 1, 2, 3, 4, 5, 6, 7);
            dayReminderInterval.setItemLabelGenerator(i -> i == 1 ? "1 Tag vorher" : i + " Tage vorher");
            this.add(dayReminderInterval);
            binder.forField(dayReminderInterval).bind(Person::getDayReminderIntervals, Person::setDayReminderIntervals);

            MultiSelectComboBox<Integer> hourReminderInterval = new MultiSelectComboBox<>("Stündliche Erinnerungen");
            hourReminderInterval.setItems(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
            hourReminderInterval.setItemLabelGenerator(i -> i == 1 ? "1 Stunde vorher" : i + " Stunden vorher");
            this.add(hourReminderInterval);
            binder.forField(hourReminderInterval).bind(Person::getHourReminderIntervals, Person::setHourReminderIntervals);

            binder.readBean(person);

            Button save = new Button("Speichern");
            save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            save.addClickListener(event -> {
                try {
                    binder.writeBean(person);
                    personsRepository.save(person);
                    Notification.show("Speichern erfolgreich").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                } catch (ValidationException e) {
                    LOGGER.error("Das Speichern der Benachrichtigungseinstellungen von {} ist Fehlgeschlagen", person.getName(), e);
                    Notification.show("Das Speichern ist fehlgeschlagen. Bitte informiere den Entwickler").addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            });
            this.add(save);
        }
    }

}