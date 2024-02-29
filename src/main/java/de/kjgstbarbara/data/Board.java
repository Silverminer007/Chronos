package de.kjgstbarbara.data;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@RequiredArgsConstructor
@Entity
public class Board {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Setter(AccessLevel.PRIVATE)
    private long id;
    @NonNull
    @ManyToOne
    private Organisation organisation;
    @NonNull
    private String title;
    private String memberTitle = "Mitglieder";
    private String dateTitle = "Termine";
    private boolean memberEnabled = true;
    private boolean dateEnabled = true;
    @ManyToMany(fetch = FetchType.EAGER)
    private List<Person> members = List.of();
    @ManyToMany(fetch = FetchType.EAGER)
    private List<Person> persons = List.of();
    @ManyToMany(fetch = FetchType.EAGER)
    private List<Date> dateList = List.of();
    @OneToOne
    private Role requiredRole;


    public boolean isAdmin(Person user) {
        return user.getRoles().contains(this.requiredRole);
    }
}