package de.kjgstbarbara.views.date;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.accordion.AccordionPanel;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.details.DetailsVariant;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.kjgstbarbara.FrontendUtils;
import de.kjgstbarbara.data.Date;
import de.kjgstbarbara.data.Feedback;
import de.kjgstbarbara.data.Person;
import de.kjgstbarbara.service.DateRepository;
import de.kjgstbarbara.service.DatesService;
import de.kjgstbarbara.service.PersonsRepository;
import de.kjgstbarbara.service.PersonsService;
import de.kjgstbarbara.views.MainNavigationView;
import jakarta.annotation.security.PermitAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.function.Supplier;

@Route(value = "date/:date/feedback", layout = MainNavigationView.class)
@PageTitle("Feedback Historie")
@PermitAll
public class DateFeedbackOverviewView extends VerticalLayout implements BeforeEnterObserver {
    private static final Logger LOGGER = LoggerFactory.getLogger(DateFeedbackOverviewView.class);

    private final PersonsRepository personsRepository;
    private final DateRepository dateRepository;

    private final Person principal;

    public DateFeedbackOverviewView(PersonsService personsService, DatesService datesService, AuthenticationContext authenticationContext) {
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
        this.removeAll();
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
        back.addClickListener(event -> UI.getCurrent().navigate(DateView.class, new RouteParameters(new RouteParam("date", date.getId()))));
        back.setIcon(VaadinIcon.ARROW_LEFT.create());
        header.add(back);
        H3 dateName = new H3(date.getTitle());
        header.add(dateName);
        this.add(header);

        NativeLabel time = new NativeLabel((date.getStart().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT))));
        this.add(time);

        NativeLabel pollPlannedFor = new NativeLabel( date.getPollScheduledFor() == null ? "Aktuell ist keine Feedback-Erinnerung geplant" : "Für den " + date.getPollScheduledFor().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)) + " ist eine Erinnerung ans Abstimmen geplant");
        pollPlannedFor.setVisible(date.getPollScheduledFor() != null && date.isPollRunning());
        this.add(pollPlannedFor);

        H4 pollIsStopped = new H4("Die Abfrage wurde beendet");
        pollIsStopped.setVisible(!date.isPollRunning());
        this.add(pollIsStopped);

        Accordion accordion = new Accordion();
        accordion.setWidthFull();

        AccordionPanel confirmedUsers = new AccordionPanel();
        confirmedUsers.addThemeVariants(DetailsVariant.FILLED, DetailsVariant.REVERSE);
        confirmedUsers.setEnabled(false);
        accordion.add(confirmedUsers);

        AccordionPanel declinedUsers = new AccordionPanel();
        declinedUsers.addThemeVariants(DetailsVariant.FILLED, DetailsVariant.REVERSE);
        declinedUsers.setEnabled(false);
        accordion.add(declinedUsers);

        AccordionPanel noFeedback = new AccordionPanel();
        noFeedback.addThemeVariants(DetailsVariant.FILLED, DetailsVariant.REVERSE);
        noFeedback.setEnabled(false);
        accordion.add(noFeedback);

        int confirmedAmount = 0;
        int declinedAmount = 0;
        int noFeedbackAmount = 0;
        for (Person p : date.getGroup().getMembers()) {
            Feedback.Status status = date.getStatusFor(p);
            Supplier<HorizontalLayout> personEntry = () -> {
                HorizontalLayout horizontalLayout = new HorizontalLayout();
                Avatar avatar = FrontendUtils.getAvatar(p);
                H4 label = new H4(avatar.getName());
                horizontalLayout.add(avatar, label);
                horizontalLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
                horizontalLayout.setAlignItems(FlexComponent.Alignment.CENTER);
                return horizontalLayout;
            };
            if (Feedback.Status.COMMITTED.equals(status)) {
                confirmedUsers.add(personEntry.get());
                confirmedUsers.add(new Paragraph());
                confirmedUsers.setEnabled(true);
                confirmedAmount++;
            } else if (Feedback.Status.CANCELLED.equals(status)) {
                declinedUsers.add(personEntry.get());
                declinedUsers.add(new Paragraph());
                declinedUsers.setEnabled(true);
                declinedAmount++;
            } else {
                noFeedback.add(personEntry.get());
                noFeedback.add(new Paragraph());
                noFeedback.setEnabled(true);
                noFeedbackAmount++;
            }
        }
        confirmedUsers.setSummaryText("Zusagen (" + confirmedAmount + ")");
        declinedUsers.setSummaryText("Absagen (" + declinedAmount + ")");
        noFeedback.setSummaryText("Keine Rückmeldung (" + noFeedbackAmount + ")");
        this.add(accordion);

        FlexLayout options = new FlexLayout();
        options.addClassName(LumoUtility.Gap.MEDIUM);
        options.setFlexWrap(FlexLayout.FlexWrap.WRAP);

        Button remindAll = new Button("Um Feedback bitten");
        remindAll.addClickListener(e -> UI.getCurrent().navigate(DateSendReminderView.class, new RouteParameters(new RouteParam("date", date.getId()))));
        remindAll.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        remindAll.setVisible(date.isPollRunning() && date.getGroup().getAdmins().contains(this.principal));
        options.add(remindAll);

        Button stopPoll = new Button("Abstimmung stoppen");
        stopPoll.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
        stopPoll.setVisible(date.isPollRunning() && date.getGroup().getAdmins().contains(this.principal));
        stopPoll.addClickListener(e -> {
            ConfirmDialog confirmDialog = new ConfirmDialog(
                    "Bist du sicher, dass du die Abfrage beenden möchtest?",
                    "Das kann nicht mehr Rückgängig gemacht werden",
                    "Abfrage beenden",
                    (confirmEvent) -> {
                        date.setPollRunning(false);
                        dateRepository.save(date);
                        stopPoll.setVisible(false);
                        remindAll.setVisible(false);
                        pollIsStopped.setVisible(true);
                        pollPlannedFor.setVisible(false);
                    });
            confirmDialog.setCancelable(true);
            confirmDialog.open();
        });
        options.add(stopPoll);

        Button history = new Button("Feedback Historie");
        history.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        history.addClickListener(e -> UI.getCurrent().navigate(DateHistoryView.class, new RouteParameters(new RouteParam("date", date.getId()))));
        options.add(history);

        this.add(options);
    }
}