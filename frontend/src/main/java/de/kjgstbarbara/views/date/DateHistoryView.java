package de.kjgstbarbara.views.date;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.avatar.AvatarVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.kjgstbarbara.FrontendUtils;
import de.kjgstbarbara.data.*;
import de.kjgstbarbara.service.*;
import de.kjgstbarbara.components.AvatarItem;
import de.kjgstbarbara.views.MainNavigationView;
import jakarta.annotation.security.PermitAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

@Route(value = "date/:date/history", layout = MainNavigationView.class)
@PageTitle("Feedback Historie")
@PermitAll
public class DateHistoryView extends VerticalLayout implements BeforeEnterObserver {
    private static final Logger LOGGER = LoggerFactory.getLogger(DateHistoryView.class);

    private final PersonsRepository personsRepository;
    private final DateRepository dateRepository;

    private final Person principal;

    public DateHistoryView(PersonsService personsService, DatesService datesService, AuthenticationContext authenticationContext) {
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

        List<Feedback> sortedFeedback = date.getFeedbackList().stream().sorted(Comparator.comparing(Feedback::getTimeStamp).reversed()).toList();
        for (Feedback feedback : sortedFeedback) {
            this.add(createBadge(feedback.getTimeStamp().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) + " Uhr"));

            HorizontalLayout action = new HorizontalLayout();
            action.setAlignItems(Alignment.CENTER);

            AvatarItem avatarItem = new AvatarItem();
            Avatar avatar = FrontendUtils.getAvatar(feedback.getPerson());
            avatar.addThemeVariants(AvatarVariant.LUMO_SMALL);
            avatarItem.setAvatar(avatar);
            avatarItem.setHeading(feedback.getPerson().getName());
            action.add(avatarItem);

            action.add(createBadge(feedback.getStatus().getReadable()));

            Icon statusIcon;
            if(Feedback.Status.COMMITTED.equals(feedback.getStatus())) {
                statusIcon = VaadinIcon.CHECK.create();
                statusIcon.setColor("#00ff00");
            } else {
                statusIcon = VaadinIcon.CLOSE.create();
                statusIcon.setColor("#ff0000");
            }
            statusIcon.addClassName(LumoUtility.IconSize.SMALL);
            action.add(statusIcon);
            this.add(action);
            this.add(new Hr());
        }
        if (sortedFeedback.isEmpty()) {
            this.add("Keine Aktivitäten");
        }
    }

    private Span createBadge(String value) {
        Span badge = new Span(value);
        badge.getElement().getThemeList().add("badge small contrast");
        badge.getStyle().set("margin-inline-start", "var(--lumo-space-xs)");
        return badge;
    }
}