package de.kjgstbarbara.data;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
    private String attachment;
    private boolean internal = true;
    @ManyToMany(fetch = FetchType.EAGER)
    private List<Person> in = new ArrayList<>();
    @ManyToMany(fetch = FetchType.EAGER)
    private List<Person> out = new ArrayList<>();
    @ManyToMany(fetch = FetchType.EAGER)
    private List<Person> dontknow = new ArrayList<>();

    @Override
    public int compareTo(Date o) {
        return start.compareTo(o.getStart());
    }
}