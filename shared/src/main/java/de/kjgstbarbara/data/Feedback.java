package de.kjgstbarbara.data;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
public class Feedback implements Comparable<Feedback> {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;
    @ManyToOne
    private Person person;
    private LocalDateTime timeStamp = LocalDateTime.now();
    private Status status;

    public Feedback(Person person, Status status) {
        this.person = person;
        this.status = status;
    }

    @Override
    public int compareTo(Feedback o) {
        return this.getTimeStamp().compareTo(o.getTimeStamp()) * -1;
    }

    @Getter
    public enum Status {
        COMMITTED("Bin dabei"), CANCELLED("Bin raus"), NONE("Weiß nicht");

        private final String readable;

        Status(String readable) {
            this.readable = readable;
        }
    }

    public static Feedback create(Person person, Status status) {
        Feedback feedback = new Feedback();
        feedback.setPerson(person);
        feedback.setStatus(status);
        return feedback;
    }
}