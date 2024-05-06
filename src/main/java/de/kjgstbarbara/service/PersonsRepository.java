package de.kjgstbarbara.service;

import de.kjgstbarbara.data.Person;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PersonsRepository extends JpaRepository<Person, Long> {
    Optional<Person> findByUsername(String username) ;

    Optional<Person> findByRegionCodeAndNationalNumber(String regionCode, long nationalNumber);

    List<Person> findBySystemAdmin(boolean systemAdmin);
}