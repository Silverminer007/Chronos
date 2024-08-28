package de.kjgstbarbara.chronos.service;

import de.kjgstbarbara.chronos.data.Group;
import de.kjgstbarbara.chronos.data.Organisation;
import de.kjgstbarbara.chronos.data.Person;
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

    default List<Group> findByNameAndPerson(String name, Person person) {
        return this.findByNameIgnoreCaseLikeAndMembersInOrNameIgnoreCaseLikeAndOrganisationAdmin(
                "%" + name + "%", List.of(person), "%" + name + "%", person);
    }

    List<Group> findByNameIgnoreCaseLikeAndMembersInOrNameIgnoreCaseLikeAndOrganisationAdmin(String name, List<Person> person, String name2, Person admin);

    List<Group> findByOrganisation(Organisation organisation);
}
