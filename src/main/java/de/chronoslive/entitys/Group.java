package de.chronoslive.entitys;

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
    private Long id;
    @NonNull
    private String name;
    private String color = generateColor();
    @ManyToOne
    private Organisation organisation;
    @ManyToMany(fetch = FetchType.EAGER)
    private List<Person> members = new ArrayList<>();
    @ManyToMany(fetch = FetchType.EAGER)
    private List<Person> admins = new ArrayList<>();

    public Group(Long id) {
        this.id = id;
    }

    public String toString() {
        return name;
    }

    public static String generateColor() {
        return "#" + Integer.toHexString(new Random().nextInt(0xffffff));
    }
}