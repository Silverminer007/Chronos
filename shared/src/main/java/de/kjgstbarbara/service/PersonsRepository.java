package de.kjgstbarbara.service;

import de.kjgstbarbara.data.Person;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PersonsRepository extends JpaRepository<Person, Long> {
    default Optional<Person> findByUsernameOrEmail(String username) {
        return Optional.ofNullable(findByUsername(username).orElseGet(() -> findByeMailAddress(username).orElse(null)));
    }

    Optional<Person> findByUsername(String username);

    Optional<Person> findByeMailAddress(String email);

    Optional<Person> findByPhoneNumber(Person.PhoneNumber phoneNumber);

}