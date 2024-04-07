package de.kjgstbarbara.data;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Objects;

@Entity
@Data
@NoArgsConstructor
public class Person {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Setter(AccessLevel.PACKAGE)
    private long id;
    private String firstName;
    private String lastName;
    private String username = "";
    private LocalDate birthDate;
    private long phoneNumber = 0L;
    private String password;

    public String toString() {
        return firstName + " " + lastName;
    }


    public int hashCode() {
        return Objects.hashCode(this.getId());
    }

    public boolean equals(Object o) {
        if (o instanceof Person p) {
            return Objects.equals(p.getId(), this.getId());
        }
        return false;
    }
}