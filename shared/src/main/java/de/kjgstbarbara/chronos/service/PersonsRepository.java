package de.kjgstbarbara.chronos.service;

import de.kjgstbarbara.chronos.data.Person;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PersonsRepository extends JpaRepository<Person, Long> {
    Optional<Person> findByUsername(String username) ;

    Optional<Person> findByPhoneNumber(Person.PhoneNumber phoneNumber);

}