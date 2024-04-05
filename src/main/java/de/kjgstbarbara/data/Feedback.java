package de.kjgstbarbara.data;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.io.Serializable;

@Entity
@Data
@NoArgsConstructor
public class Feedback {
    @EmbeddedId
    private Key key;
    private Status status;

    public enum Status {
        IN, OUT, DONTKNOW;
    }

    public static Feedback create(Person person, Date date, Status status) {
        Feedback feedback = new Feedback();
        feedback.setKey(Key.create(person, date));
        feedback.setStatus(status);
        return feedback;
    }

    @Data
    @Embeddable
    public static class Key implements Serializable {
        @ManyToOne
        private Person person;
        @ManyToOne
        private Date date;
        public static @NonNull Key create(Person person, Date date) {
            Key key = new Key();
            key.setDate(date);
            key.setPerson(person);
            return key;
        }
    }
}