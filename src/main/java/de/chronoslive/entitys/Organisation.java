package de.chronoslive.entitys;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
public class Organisation {
    private static final Logger LOGGER = LoggerFactory.getLogger(Organisation.class);
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Id
    private Long id;
    private String name;
    @ManyToOne
    private Person admin;
    @ManyToMany(fetch = FetchType.EAGER)
    private List<Person> members = new ArrayList<>();
    @ManyToMany(fetch = FetchType.EAGER)
    private List<Person> membershipRequests = new ArrayList<>();

    public Organisation(Long id) {
        this.id = id;
    }

    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Organisation org && this.id == org.id;
    }

    @Override
    public int hashCode() {
        return (int)(long) this.id;
    }
}