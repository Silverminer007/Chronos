package de.kjgstbarbara.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.accordion.AccordionPanel;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.avatar.AvatarGroup;
import com.vaadin.flow.component.avatar.AvatarGroupVariant;
import com.vaadin.flow.component.avatar.AvatarVariant;
import com.vaadin.flow.component.details.DetailsVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.kjgstbarbara.FrontendUtils;
import de.kjgstbarbara.data.Date;
import de.kjgstbarbara.data.Feedback;
import de.kjgstbarbara.data.Person;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

public class FeedbackAvatars extends HorizontalLayout {
    private final AvatarGroup avatarIcons = new AvatarGroup();
    private final NativeLabel noAvatars = new NativeLabel("Niemand");
    private final Feedback.Status status;

    public FeedbackAvatars(Feedback.Status status, Date date) {
        this.status = status;

        this.setJustifyContentMode(JustifyContentMode.START);
        this.setAlignItems(Alignment.CENTER);
        this.setWidthFull();

        this.add(new NativeLabel(switch (status) {
            case COMMITTED -> "Zusagen:";
            case CANCELLED -> "Absagen:";
            case NONE -> "Ausstehend:";
        }));

        this.avatarIcons.setMaxItemsVisible(7);
        this.avatarIcons.addThemeVariants(AvatarGroupVariant.LUMO_SMALL);
        this.add(this.avatarIcons);
        this.add(this.noAvatars);
        this.update(date);

        this.addClickListener(event -> createFeedbackOverviewDialog(date).open());
    }

    private Dialog createFeedbackOverviewDialog(Date date) {
        Dialog dialog = new Dialog();
        dialog.setMinWidth("300px");
        dialog.setMaxHeight("600px");

        dialog.add(createFeedbackOverview(date));
        return dialog;
    }

    private Component createFeedbackOverview(Date date) {
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
                horizontalLayout.setJustifyContentMode(JustifyContentMode.START);
                horizontalLayout.setAlignItems(Alignment.CENTER);
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

        AccordionPanel history = new AccordionPanel();
        history.addThemeVariants(DetailsVariant.FILLED, DetailsVariant.REVERSE);
        history.setEnabled(true);
        history.setSummaryText("Historie");
        history.add(createHistory(date));
        accordion.add(history);
        return accordion;
    }

    private VerticalLayout createHistory(Date date) {
        VerticalLayout history = new VerticalLayout();
        List<Feedback> sortedFeedback = date.getFeedbackList().stream().sorted(Comparator.comparing(Feedback::getTimeStamp).reversed()).toList();
        for (Feedback feedback : sortedFeedback) {
            history.add(createBadge(feedback.getTimeStamp().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) + " Uhr"));

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
                statusIcon = VaadinIcon.THUMBS_UP.create();
                statusIcon.setColor("#00ff00");
            } else {
                statusIcon = VaadinIcon.THUMBS_DOWN.create();
                statusIcon.setColor("#ff0000");
            }
            statusIcon.addClassName(LumoUtility.IconSize.SMALL);
            action.add(statusIcon);
            history.add(action);
            history.add(new Hr());
        }
        if (sortedFeedback.isEmpty()) {
            history.add("Keine Aktivitäten");
        }
        return history;
    }

    private Span createBadge(String value) {
        Span badge = new Span(value);
        badge.getElement().getThemeList().add("badge small contrast");
        badge.getStyle().set("margin-inline-start", "var(--lumo-space-xs)");
        return badge;
    }

    public void update(Date date) {
        this.avatarIcons.setItems(FrontendUtils.getAvatars(date, status));
        boolean avatars = !this.avatarIcons.getItems().isEmpty();
        this.avatarIcons.setVisible(avatars);
        this.noAvatars.setVisible(!avatars);
    }
}
