package de.kjgstbarbara.chronos.views.dates;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.spring.security.AuthenticationContext;
import de.kjgstbarbara.chronos.data.Date;
import de.kjgstbarbara.chronos.data.Person;
import de.kjgstbarbara.chronos.messaging.MessageFormatter;
import de.kjgstbarbara.chronos.service.DateRepository;
import de.kjgstbarbara.chronos.service.DatesService;
import de.kjgstbarbara.chronos.service.PersonsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@AnonymousAllowed
@Route("date/:dateID/vote/:answer/:personID")
public class VoteDateView extends VerticalLayout implements BeforeEnterObserver {
    @Autowired
    private DatesService datesService;
    @Autowired
    private PersonsService personsService;
    @Autowired
    private AuthenticationContext authenticationContext;

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        Person principal = authenticationContext.getAuthenticatedUser(UserDetails.class)
                .flatMap(userDetails -> personsService.getPersonsRepository().findByUsername(userDetails.getUsername()))
                .orElse(null);
        DateRepository dateRepository = datesService.getDateRepository();
        Date date = beforeEnterEvent.getRouteParameters().get("dateID").map(Long::valueOf).flatMap(dateRepository::findById).orElse(null);
        if (date != null) {
            boolean pollRunning = date.isPollRunning() && LocalDateTime.now(ZoneOffset.UTC).isBefore(date.getStartAtTimezone(ZoneOffset.UTC));
            if (pollRunning) {
                int answer = beforeEnterEvent.getRouteParameters().get("answer").map(Integer::parseInt).orElse(0);
                if (answer > 0) {
                    Person person = beforeEnterEvent.getRouteParameters().get("personID").map(Long::valueOf).flatMap(personsService.getPersonsRepository()::findById).orElse(null);
                    if (person != null) {
                        this.removeAll();
                        MessageFormatter messageFormatter = new MessageFormatter().date(date).person(person);
                        this.add(new H3(messageFormatter.format("#DATE_TITLE am #DATE_START_DATE. Du bist " + (answer == 2 ? "nicht " : "") + " dabei")));
                        this.add("Bitte gib deinen Nachnamen ein, um deine Identität zu bestätigen");
                        TextField lastName = new TextField();
                        Button confirm = new Button(VaadinIcon.CHECK.create());
                        confirm.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                        confirm.addClickShortcut(Key.ENTER);
                        confirm.addClickListener(event -> {
                            if (lastName.getValue().equals(person.getLastName())) {
                                Date.Feedback feedback = new Date.Feedback(person, answer == 1 ? Date.Feedback.Status.COMMITTED : Date.Feedback.Status.CANCELLED);
                                date.addFeedback(feedback);
                                dateRepository.save(date);
                                if (!authenticationContext.isAuthenticated()) {
                                    this.removeAll();
                                    this.add(new H3(person.getFirstName() + ", deine Rückmeldung wurde gespeichert. Du kannst dieses Fenster jetzt schließen. Die anderen können sehen, dass du " + (answer == 2 ? "nicht " : "") + " dabei bist"));
                                } else {
                                    event.getSource().getUI().ifPresent(ui -> ui.navigate("date/" + date.getId()));
                                }
                            } else {
                                lastName.setInvalid(true);
                                lastName.setErrorMessage("Die Nachnamen stimmen nicht überein");
                            }
                        });
                        HorizontalLayout confirmIdentity = new HorizontalLayout(lastName, confirm);
                        this.add(confirmIdentity);
                        if(person.equals(principal)) {
                            confirm.click();
                        }
                        return;
                    }
                }
                beforeEnterEvent.rerouteTo("date/" + date.getId());
            } else {
                this.removeAll();
                this.add(new H3("Die Zeit zum Abstimmen für " + date.getTitle() + " ist bereits abgelaufen. Bitte kläre mögliche Änderungen persönlich ab"));
            }
            return;
        }
        beforeEnterEvent.rerouteTo("calendar");
    }
}