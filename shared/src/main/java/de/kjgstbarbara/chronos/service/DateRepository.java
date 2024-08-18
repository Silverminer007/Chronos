package de.kjgstbarbara.chronos.service;

import de.kjgstbarbara.chronos.data.*;
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

    default Stream<Date> findByStartBetweenAndTitleLikeAndGroupMembersIn(LocalDateTime start, LocalDateTime end, String searchTerm, Person... persons) {
        return findByStartBetweenAndTitleLikeAndGroupMembersIn(start, end, "%" + searchTerm + "%", List.of(persons)).stream();
    }

    List<Date> findByStartBetweenAndTitleLikeAndGroupMembersIn(LocalDateTime start, LocalDateTime end, String searchTerm, List<Person> personList);

    List<Date> findByLinkedTo(long linkedTo);

    List<Date> findByStartBetween(LocalDateTime after, LocalDateTime before);
    List<Date> findByPollScheduledFor(LocalDate localDate);
}
