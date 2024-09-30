package de.kjgstbarbara.service;

import de.kjgstbarbara.data.Date;
import de.kjgstbarbara.data.Group;
import de.kjgstbarbara.data.Person;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

public interface DateRepository extends JpaRepository<Date, Long> {
    List<Date> findByGroup(Group group);

    default Stream<Date> findByStartBetweenAndGroupMembersIn(LocalDateTime start, LocalDateTime end, Person... persons) {
        return findByStartBetweenAndGroupMembersIn(start, end, List.of(persons)).stream();
    }

    List<Date> findByStartBetweenAndGroupMembersIn(LocalDateTime start, LocalDateTime end, List<Person> personList);

    List<Date> findByStartBetween(LocalDateTime after, LocalDateTime before);

    List<Date> findByPollScheduledFor(LocalDate localDate);
}
