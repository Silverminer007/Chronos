package de.kjgstbarbara.chronos.service;

import de.kjgstbarbara.chronos.data.Group;
import de.kjgstbarbara.chronos.data.Organisation;
import de.kjgstbarbara.chronos.data.Person;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroupRepository extends JpaRepository<Group, Long> {

    default List<Group> findByMembersIn(Person... person) {
        return this.findByMembersIn(List.of(person));
    }

    List<Group> findByMembersIn(List<Person> person);

    default List<Group> findByAdminsIn(Person... person) {
        return this.findByAdminsIn(List.of(person));
    }

    List<Group> findByAdminsIn(List<Person> person);

    List<Group> findByNameIgnoreCaseLikeAndMembersIn(String title, List<Person> person, PageRequest of);

    default List<Group> findByOrganisation(Organisation organisation) {
        return List.of();
    }
}
