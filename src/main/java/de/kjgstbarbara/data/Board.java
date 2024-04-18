package de.kjgstbarbara.data;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@Entity
public class Board {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Setter(AccessLevel.PRIVATE)
    private long id;
    @NonNull
    private String title;
    @ManyToMany(fetch = FetchType.EAGER)
    private final List<Person> members = new ArrayList<>();
    @ManyToMany(fetch = FetchType.EAGER)
    private final List<Person> admins = new ArrayList<>();
    @ManyToMany(fetch = FetchType.EAGER)
    private final List<Person> requests = new ArrayList<>();
    private String color = "0x0000ff";

    public String toString() {
        return title;
    }
}