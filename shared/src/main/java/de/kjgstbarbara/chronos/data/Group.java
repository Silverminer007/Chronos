package de.kjgstbarbara.chronos.data;

import jakarta.persistence.*;
import lombok.*;

import java.util.*;

@Data
@NoArgsConstructor
@Entity
@Table(name = "people-group")
public class Group {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Setter(AccessLevel.PRIVATE)
    private long id;
    @NonNull
    private String name;
    private String color = generateColor();
    @ManyToOne
    private Organisation organisation;
    @ManyToMany(fetch = FetchType.EAGER)
    private Set<Person> members = new HashSet<>();
    @ManyToMany(fetch = FetchType.EAGER)
    private Set<Person> visible = new HashSet<>();
    @ManyToMany(fetch = FetchType.EAGER)
    private Set<Person> admins = new HashSet<>();

    public String toString() {
        return name;
    }

    public static String generateColor() {
        return "#" + Integer.toHexString(new Random().nextInt(0xffffff));
    }
}