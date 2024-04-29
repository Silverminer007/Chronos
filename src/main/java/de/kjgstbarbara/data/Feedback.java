package de.kjgstbarbara.data;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.io.Serializable;
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

    @Override
    public int compareTo(Feedback o) {
        return this.getTimeStamp().compareTo(o.getTimeStamp()) * -1;
    }

    @Getter
    public enum Status {
        IN("Bin dabei"), OUT("Bin dabei"), DONTKNOW("Weiß nicht");

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