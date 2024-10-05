package de.kjgstbarbara.service;

import de.kjgstbarbara.data.Organisation;
import de.kjgstbarbara.data.Person;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrganisationRepository extends JpaRepository<Organisation, Long> {
    List<Organisation> findByNameIgnoreCaseLikeAndMembersIn(String title, List<Person> person, PageRequest of);

    List<Organisation> findByNameIgnoreCaseLikeAndMembersIn(String title, List<Person> person);

    List<Organisation> findByMembersIn(List<Person> person);

    default List<Organisation> findByMembersIn(Person... person) {
        return this.findByMembersIn(List.of(person));
    }

    List<Organisation> findByMembershipRequestsIn(List<Person> person);

    default List<Organisation> findByMembershipRequestsIn(Person... person) {
        return this.findByMembershipRequestsIn(List.of(person));
    }

    List<Organisation> findByAdmin(Person admin);
}