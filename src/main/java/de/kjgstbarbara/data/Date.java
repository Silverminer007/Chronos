package de.kjgstbarbara.data;

import com.google.common.collect.ImmutableList;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Data
@Entity
public class Date implements Comparable<Date> {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Setter(AccessLevel.PRIVATE)
    private long id;
    private String title;
    private LocalDateTime start;
    private LocalDateTime end;
    private String attachment;
    @Deprecated
    private boolean internal = false;
    private boolean publish = false;
    private boolean pollRunning = true;
    @ManyToOne
    private Group group;
    @OneToMany(fetch = FetchType.EAGER)
    private List<Feedback> feedbackList = new ArrayList<>();
    private LocalDate pollScheduledFor;

    @Override
    public int compareTo(Date o) {
        return start.compareTo(o.getStart());
    }

    public Feedback.Status getStatusFor(Person person) {
        Collections.sort(feedbackList);
        for (Feedback f : feedbackList) {
            if (f.getPerson().getId() == person.getId()) {
                return f.getStatus();
            }
        }
        return Feedback.Status.DONTKNOW;
    }

    public List<Feedback> getFeedbackList() {
        return ImmutableList.copyOf(feedbackList);
    }

    public void addFeedback(Feedback feedback) {
        if (pollRunning) {
            this.feedbackList.add(feedback);
        }
    }
}