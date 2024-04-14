package de.kjgstbarbara.views.components;

import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.accordion.AccordionPanel;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.avatar.AvatarGroup;
import com.vaadin.flow.component.avatar.AvatarGroupVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.details.DetailsVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import de.kjgstbarbara.data.Date;
import de.kjgstbarbara.data.Feedback;
import de.kjgstbarbara.data.Person;
import de.kjgstbarbara.service.FeedbackRepository;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.function.Supplier;

public class DateWidget extends VerticalLayout {

    private final FeedbackRepository feedbackRepository;
    private final Date date;
    private final Person person;
    private boolean expanded;

    public DateWidget(Date date, FeedbackRepository feedbackRepository, Person person) {
        this.feedbackRepository = feedbackRepository;
        this.date = date;
        this.person = person;
        this.update();
    }

    private void update() {
        Feedback feedback = feedbackRepository.findById(Feedback.Key.create(this.person, date)).orElse(Feedback.create(this.person, date, null));
        Feedback.Status status = feedback.getStatus();
        this.removeAll();
        HorizontalLayout primaryLine = new HorizontalLayout();
        primaryLine.setWidthFull();
        H3 title = new H3();
        title.addClickListener(event -> this.toggle());
        title.setText(date.getTitle());
        primaryLine.add(title);
        primaryLine.setJustifyContentMode(JustifyContentMode.CENTER);
        primaryLine.setAlignItems(Alignment.CENTER);
        HorizontalLayout rightSidePrimaryLine = new HorizontalLayout();
        H3 startDate = new H3(date.getStart().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)));
        startDate.addClickListener(event -> this.toggle());
        if (!expanded) {
            primaryLine.add(startDate);
        }
        Button edit = new Button(VaadinIcon.PENCIL.create());
        edit.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        edit.setVisible(date.getBoard().getAdmins().contains(this.person));
        rightSidePrimaryLine.add(edit);
        rightSidePrimaryLine.setWidthFull();
        rightSidePrimaryLine.setVerticalComponentAlignment(Alignment.START);
        rightSidePrimaryLine.setAlignItems(Alignment.END);
        rightSidePrimaryLine.setJustifyContentMode(JustifyContentMode.END);
        primaryLine.add(rightSidePrimaryLine);
        this.add(primaryLine);
        if (this.expanded) {
            H4 time = new H4((date.getStart().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM))) +
                    (date.getEnd() == null ? "" : (" - " + date.getEnd().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)))));
            this.add(time);

            HorizontalLayout feedbacks = new HorizontalLayout();
            AvatarGroup confirmed = new AvatarGroup();
            confirmed.setWidth("50%");
            confirmed.setMaxItemsVisible(4);
            confirmed.addThemeVariants(AvatarGroupVariant.LUMO_SMALL);
            AvatarGroup declined = new AvatarGroup();
            declined.setWidth("50%");
            declined.setMaxItemsVisible(4);
            declined.addThemeVariants(AvatarGroupVariant.LUMO_SMALL);
            List<Feedback> feedbacksForDate = feedbackRepository.findByDate(date);
            for (Feedback f : feedbacksForDate) {
                if (Feedback.Status.IN.equals(f.getStatus())) {
                    confirmed.add(f.getKey().getPerson().getAvatarGroupItem());
                } else if (Feedback.Status.OUT.equals(f.getStatus())) {
                    declined.add(f.getKey().getPerson().getAvatarGroupItem());
                }
            }
            feedbacks.setWidthFull();
            feedbacks.add(confirmed, declined);
            feedbacks.addClickListener(event -> {
                Dialog dialog = new Dialog("Rückmeldungen zu \"" + date.getTitle() + "\"");
                Accordion accordion = new Accordion();
                AccordionPanel confirmedUsers = new AccordionPanel("Zusagen (" + confirmed.getItems().size() + ")");
                confirmedUsers.addThemeVariants(DetailsVariant.FILLED, DetailsVariant.REVERSE);
                if(confirmed.getItems().isEmpty()) {
                    confirmedUsers.setEnabled(false);
                }
                AccordionPanel declinedUsers = new AccordionPanel("Absagen (" + declined.getItems().size() + ")");
                declinedUsers.addThemeVariants(DetailsVariant.FILLED, DetailsVariant.REVERSE);
                if(declined.getItems().isEmpty()) {
                    declinedUsers.setEnabled(false);
                }
                AccordionPanel noFeedback = new AccordionPanel("Keine Rückmeldung (" + (date.getBoard().getMembers().size() - confirmed.getItems().size() - declined.getItems().size()) + ")");
                noFeedback.addThemeVariants(DetailsVariant.FILLED, DetailsVariant.REVERSE);
                if(date.getBoard().getMembers().size() - confirmed.getItems().size() - declined.getItems().size() == 0) {
                    noFeedback.setEnabled(false);
                }
                accordion.add(confirmedUsers);
                accordion.add(declinedUsers);
                accordion.add(noFeedback);
                for(Feedback f : feedbacksForDate) {
                    Supplier<HorizontalLayout> p = () -> {
                        HorizontalLayout horizontalLayout = new HorizontalLayout();
                        Avatar avatar = f.getKey().getPerson().getAvatar();
                        H4 label = new H4(avatar.getName());
                        horizontalLayout.add(avatar, label);
                        horizontalLayout.setJustifyContentMode(JustifyContentMode.START);
                        horizontalLayout.setAlignItems(Alignment.CENTER);
                        return horizontalLayout;
                    };
                    if (Feedback.Status.IN.equals(f.getStatus())) {
                        confirmedUsers.add(p.get());
                    } else if (Feedback.Status.OUT.equals(f.getStatus())) {
                        declinedUsers.add(p.get());
                    } else {
                        noFeedback.add(p.get());
                    }
                }
                dialog.add(accordion);
                dialog.open();
            });
            this.add(feedbacks);

            HorizontalLayout footer = new HorizontalLayout();
            Button confirm = new Button("Zusagen");
            confirm.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
            confirm.setEnabled(!Feedback.Status.IN.equals(status));
            confirm.setWidth("50%");
            confirm.addClickListener(event -> {
                feedback.setStatus(Feedback.Status.IN);
                feedbackRepository.save(feedback);
                this.update();
            });
            Button decline = new Button("Absagen");
            decline.addClickListener(event -> {
                feedback.setStatus(Feedback.Status.OUT);
                feedbackRepository.save(feedback);
                this.update();
            });
            decline.addThemeVariants(ButtonVariant.LUMO_ERROR);
            decline.setEnabled(!Feedback.Status.OUT.equals(status));
            decline.setWidth("50%");
            footer.add(confirm, decline);
            footer.setWidthFull();
            this.add(footer);
            this.setHeight(title.getHeight() + time.getHeight() + confirm.getHeight() + confirmed.getHeight());
        } else {
            this.setHeight(title.getHeight());
        }
    }

    public void toggle() {
        this.expanded = !this.expanded;
        this.update();
    }
}
