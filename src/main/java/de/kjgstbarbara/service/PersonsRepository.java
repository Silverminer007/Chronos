package de.kjgstbarbara.service;

import de.kjgstbarbara.data.Board;
import de.kjgstbarbara.data.Person;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PersonsRepository extends JpaRepository<Person, Long> {
    default Optional<Person> findByUsername(String username) {
        return findAll().stream().filter(person -> person.getUsername().equals(username)).findAny();
    }

    default Optional<Person> findByPhoneNumber(long phoneNumber) {
        return findAll().stream().filter(person -> person.getPhoneNumber() == phoneNumber).findAny();
    }
}