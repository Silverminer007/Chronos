package de.kjgstbarbara.views.components;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import de.kjgstbarbara.data.Date;
import de.kjgstbarbara.data.Feedback;
import de.kjgstbarbara.data.Person;
import de.kjgstbarbara.service.FeedbackRepository;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

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
        primaryLine.addClickListener(event -> this.toggle());
        primaryLine.setWidthFull();
        H3 title = new H3();
        primaryLine.add(title);
        if (this.expanded) {
            title.setText(date.getTitle());
        } else {
            title.setText(date.getTitle() + " " + date.getStart().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)));
        }
        this.add(primaryLine);
        if (this.expanded) {
            H4 time = new H4(date.getStart().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)));
            this.add(time);
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
            this.setHeight(title.getHeight() + time.getHeight() + confirm.getHeight());
        } else {
            this.setHeight(title.getHeight());
        }
    }

    public void toggle() {
        this.expanded = !this.expanded;
        this.update();
    }
}
