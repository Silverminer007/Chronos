package de.kjgstbarbara.views;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.validator.IntegerRangeValidator;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;
import de.kjgstbarbara.data.Person;
import de.kjgstbarbara.service.PersonsRepository;
import de.kjgstbarbara.service.PersonsService;
import de.kjgstbarbara.views.nav.MainNavigationView;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.core.userdetails.UserDetails;

@Route(value = "notifications", layout = MainNavigationView.class)
@PageTitle("Benachrichtigungen")
@PermitAll
public class NotificationSettingsView extends VerticalLayout {

    public NotificationSettingsView(PersonsService personsService, AuthenticationContext authenticationContext) {
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

            Checkbox whatsapp = new Checkbox("WhatsApp");
            binder.forField(whatsapp).bind(Person::isWhatsappNotifications, Person::setWhatsappNotifications);
            form.add(whatsapp);

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

            Checkbox weekBefore = new Checkbox("7 Tage vorher");
            binder.forField(weekBefore).bind(Person::isRemindOneWeekBefore, Person::setRemindOneWeekBefore);
            form.add(weekBefore);

            Checkbox twoDaysBefore = new Checkbox("2 Tage vorher");
            binder.forField(twoDaysBefore).bind(Person::isRemindTwoDaysBefore, Person::setRemindTwoDaysBefore);
            form.add(twoDaysBefore);

            Checkbox dayBefore = new Checkbox("1 Tag vorher");
            binder.forField(dayBefore).bind(Person::isRemindOneDayBefore, Person::setRemindOneDayBefore);
            form.add(dayBefore);

            Checkbox sixHoursBefore = new Checkbox("6 Stunden vorher");
            binder.forField(sixHoursBefore).bind(Person::isRemindSixHoursBefore, Person::setRemindSixHoursBefore);
            form.add(sixHoursBefore);

            Checkbox twoHoursBefore = new Checkbox("2 Stunden vorher");
            binder.forField(twoHoursBefore).bind(Person::isRemindTwoHoursBefore, Person::setRemindTwoHoursBefore);
            form.add(twoHoursBefore);

            Checkbox hourBefore = new Checkbox("1 Stunde vorher");
            binder.forField(hourBefore).bind(Person::isRemindOneHourBefore, Person::setRemindOneHourBefore);
            form.add(hourBefore);

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