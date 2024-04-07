package de.kjgstbarbara.data;

import jakarta.persistence.*;
import lombok.*;

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
    private List<Person> members;
    @ManyToMany(fetch = FetchType.EAGER)
    private List<Person> admins;
}