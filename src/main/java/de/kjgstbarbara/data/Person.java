package de.kjgstbarbara.data;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
public class Person {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Setter(AccessLevel.PACKAGE)
    private long id;
    @NonNull
    @ManyToOne
    private Organisation organisation;
    @NonNull
    private String firstName;
    @NonNull
    private String lastName;
    @NonNull
    private String username = "";
    @NonNull
    private LocalDate birthDate;
    private long phoneNumber = 0L;
    private String password;
    @ManyToMany(fetch = FetchType.EAGER)
    private List<Role> roles = new ArrayList<>();

    public String toString() {
        return firstName + " " + lastName;
    }
}