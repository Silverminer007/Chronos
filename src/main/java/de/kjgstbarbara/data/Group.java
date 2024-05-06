package de.kjgstbarbara.data;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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
    private String title;
    private String color = generateColor();
    @ManyToMany(fetch = FetchType.EAGER)
    private final List<Person> members = new ArrayList<>();
    @ManyToMany(fetch = FetchType.EAGER)
    private final List<Person> admins = new ArrayList<>();
    @ManyToMany(fetch = FetchType.EAGER)
    private final List<Person> requests = new ArrayList<>();

    public String toString() {
        return title;
    }

    public static String generateColor() {
        return "#" + Integer.toHexString(new Random().nextInt(0xffffff));
    }
}