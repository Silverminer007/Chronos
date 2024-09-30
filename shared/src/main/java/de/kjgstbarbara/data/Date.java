package de.kjgstbarbara.data;

import com.google.common.collect.ImmutableList;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Data
@Entity
@NoArgsConstructor
public class Date implements Comparable<Date> {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Setter(AccessLevel.PRIVATE)
    private long id;
    private String title;
    private LocalDateTime start;
    private LocalDateTime end;
    private String venue;
    private String notes;
    private boolean publish = false;
    @ManyToOne
    private Group group;
    @OneToMany(fetch = FetchType.EAGER)
    private List<Feedback> feedbackList = new ArrayList<>();
    /* Wiederholungsinformationen */
    private long linkedTo = -1;
    private int repetitionRule;// Alle x Tage
    private LocalDate endOfRepetition;
    /* Ende Wiederholungsinformationen */
    private LocalDate pollScheduledFor;
    private boolean pollRunning = true;
    private LocalDate dateCancelled = null;
    @ElementCollection(fetch = FetchType.EAGER)
    private List<Information> information;

    public Date(Date date) {
        this.title = date.getTitle();
        this.start = date.getStart();
        this.end = date.getEnd();
        this.publish = date.isPublish();
        this.group = date.getGroup();
        this.notes = date.getNotes();
        this.venue = date.getVenue();
        this.linkedTo = date.getLinkedTo();
        this.repetitionRule = date.getRepetitionRule();
        this.endOfRepetition = date.getEndOfRepetition();
    }

    @Override
    public int compareTo(Date o) {
        return start.compareTo(o.getStart());
    }

    public boolean equals(Object o) {
        if (o instanceof Date date) {
            return this.getId() == date.getId();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (int) this.getId();
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

    public LocalDateTime getStartAtTimezone(ZoneId timezone) {
        return this.getStart().atZone(ZoneOffset.UTC).withZoneSameInstant(timezone).toLocalDateTime();
    }

    public LocalDateTime getEndAtTimezone(ZoneId timezone) {
        return this.getEnd().atZone(ZoneOffset.UTC).withZoneSameInstant(timezone).toLocalDateTime();
    }

    public boolean isPollRunning() {
        return this.pollRunning && this.dateCancelled == null;
    }

    @Data
    @Embeddable
    public static class Information implements Comparable<Information> {
        @ManyToOne
        private Person informationSender;
        private LocalDateTime informationTime;
        private String informationText;

        @Override
        public int compareTo(Date.Information o) {
            if (this.informationTime == null || o.informationTime == null) {
                return 0;
            }
            return o.informationTime.compareTo(this.informationTime);
        }
    }
}