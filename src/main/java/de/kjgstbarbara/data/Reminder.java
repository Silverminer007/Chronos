package de.kjgstbarbara.data;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Data
@Entity
@NoArgsConstructor
public class Reminder implements Comparable<Reminder> {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;
    private int amount;
    private ChronoUnit chronoUnit;
    @ManyToOne
    private Person person;

    public boolean matches(LocalDateTime start, int remindMeTime) {
        return LocalDateTime.now().until(start, chronoUnit) == amount && // Der Abstand ist richtig
                (chronoUnit.equals(ChronoUnit.HOURS) || LocalDateTime.now().getHour() == remindMeTime);// Und es ist entweder ein Stundengenauer Abstand oder die Stunde passt
    }

    public boolean equals(Object o) {
        if(o instanceof Reminder r) {
            return this.amount == r.amount && this.chronoUnit.equals(r.chronoUnit) && this.person.getId() == r.person.getId();
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash += 23 * amount;
        hash += 19 * chronoUnit.hashCode();
        hash += (int) (17 * person.getId());
        return hash;
    }

    @Override
    public int compareTo(Reminder o) {
        return (this.chronoUnit == ChronoUnit.DAYS ? this.amount * 24 : this.amount) - (o.chronoUnit == ChronoUnit.DAYS ? o.amount * 24 : o.amount);
    }
}