package de.kjgstbarbara.service;

import de.kjgstbarbara.data.Group;
import de.kjgstbarbara.data.Person;
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

    List<Group> findByTitleIgnoreCaseLike(String title, PageRequest of);
}
