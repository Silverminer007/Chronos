package de.kjgstbarbara.chronos.data;

import com.google.common.collect.ImmutableList;
import jakarta.persistence.*;
import lombok.*;

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
    private String attachment;
    private boolean publish = false;
    @ManyToOne
    private Group group;
    @ElementCollection(fetch = FetchType.EAGER)
    private List<Date.Feedback> feedbackList = new ArrayList<>();
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
        this.attachment = date.getAttachment();
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
            if (f.getFeedbackSender().getId() == person.getId()) {
                return f.getFeedbackStatus();
            }
        }
        return Feedback.Status.NONE;
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
            if(this.informationTime == null || o.informationTime == null) {
                return 0;
            }
            return o.informationTime.compareTo(this.informationTime);
        }
    }

    @Data
    @Embeddable
    public static class Feedback implements Comparable<Feedback> {
        @ManyToOne
        private Person feedbackSender;
        private LocalDateTime feedbackTimestamp = LocalDateTime.now();
        private Status feedbackStatus;

        public Feedback() {

        }

        public Feedback(Person person, Status status) {
            this.feedbackSender = person;
            this.feedbackStatus = status;
        }

        @Override
        public int compareTo(Feedback o) {
            return this.getFeedbackTimestamp().compareTo(o.getFeedbackTimestamp()) * -1;
        }

        @Getter
        public enum Status {
            COMMITTED("Bin dabei"), CANCELLED("Bin raus"), NONE("Weiß nicht");

            private final String readable;

            Status(String readable) {
                this.readable = readable;
            }
        }
    }
}