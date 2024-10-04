package de.kjgstbarbara.views.date;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.kjgstbarbara.data.Date;
import de.kjgstbarbara.data.Person;
import de.kjgstbarbara.messaging.MessageSender;
import de.kjgstbarbara.messaging.Messages;
import de.kjgstbarbara.service.DateRepository;
import de.kjgstbarbara.service.DatesService;
import de.kjgstbarbara.service.PersonsRepository;
import de.kjgstbarbara.service.PersonsService;
import de.kjgstbarbara.views.MainNavigationView;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

@Route(value = "date/:date/remind", layout = MainNavigationView.class)
@PageTitle("Feedback Historie")
@PermitAll
public class DateSendReminderView extends VerticalLayout implements BeforeEnterObserver {

    private final PersonsRepository personsRepository;
    private final DateRepository dateRepository;

    private final Person principal;

    public DateSendReminderView(PersonsService personsService, DatesService datesService, AuthenticationContext authenticationContext) {
        this.personsRepository = personsService.getPersonsRepository();
        this.dateRepository = datesService.getDateRepository();
        this.principal = authenticationContext.getAuthenticatedUser(UserDetails.class)
                .flatMap(userDetails -> personsRepository.findByUsername(userDetails.getUsername()))
                .orElse(null);
        if (principal == null) {
            authenticationContext.logout();
        }
        this.setSizeFull();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        Date date = beforeEnterEvent.getRouteParameters().get("date").map(Long::valueOf).flatMap(dateRepository::findById).orElse(null);
        if (date == null || !date.getGroup().getMembers().contains(principal)) {
            beforeEnterEvent.rerouteTo("");
            return;
        }
        HorizontalLayout header = new HorizontalLayout();
        header.setJustifyContentMode(JustifyContentMode.CENTER);
        Button back = new Button();
        back.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        back.setAriaLabel("Vorherige Seite");
        back.addClickListener(event -> UI.getCurrent().navigate(DateFeedbackOverviewView.class, new RouteParameters(new RouteParam("date", date.getId()))));
        back.setIcon(VaadinIcon.ARROW_LEFT.create());
        header.add(back);
        H3 dateName = new H3(date.getTitle());
        header.add(dateName);
        this.add(header);

        this.add(new H4("Erinnerung planen"));
        if (date.getPollScheduledFor() != null && date.getPollScheduledFor().isAfter(LocalDate.now())) {
            this.add(new NativeLabel("Für den " + date.getPollScheduledFor().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)) + " ist aktuell eine Erinnerung geplant"));
        }
        FlexLayout scheduleReminderLayout = new FlexLayout();
        scheduleReminderLayout.addClassName(LumoUtility.Gap.MEDIUM);
        scheduleReminderLayout.setFlexWrap(FlexLayout.FlexWrap.WRAP);
        DatePicker datePicker = new DatePicker();
        datePicker.setValue(LocalDate.now().plusDays(1));
        scheduleReminderLayout.add(datePicker);
        Button scheduleReminder = new Button("Erinnerung planen");
        scheduleReminder.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        scheduleReminder.addClickListener(e -> {
            if (datePicker.getValue() == null) {
                datePicker.setInvalid(true);
                datePicker.setErrorMessage("Kein Datum ausgewählt");
            } else if (datePicker.getValue().isBefore(LocalDate.now())) {
                datePicker.setInvalid(true);
                datePicker.setErrorMessage("Das Datum muss in der Zukunft liegen");
            } else if (datePicker.getValue().isAfter(date.getStart().toLocalDate())) {
                datePicker.setInvalid(true);
                datePicker.setErrorMessage("Die Abfrage sollte vor beginn des Termins gesendet werden");
            } else {
                date.setPollScheduledFor(datePicker.getValue());
                this.dateRepository.save(date);
                UI.getCurrent().navigate(DateFeedbackOverviewView.class, new RouteParameters(new RouteParam("date", date.getId())));
            }
        });
        scheduleReminderLayout.add(scheduleReminder);
        this.add(scheduleReminderLayout);
        this.add(new Hr());
        Button remindNow = new Button("Jetzt erinnern");
        remindNow.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        remindNow.addClickListener(e -> {
            for(Person member : date.getGroup().getMembers()) {
                new MessageSender(member).date(date).person(member).send(Messages.DATE_POLL);
            }
            UI.getCurrent().navigate(DateFeedbackOverviewView.class, new RouteParameters(new RouteParam("date", date.getId())));
            Notification.show("Die Abfrage wurde erfolgreich verschickt")
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });
        this.add(remindNow);
    }
}