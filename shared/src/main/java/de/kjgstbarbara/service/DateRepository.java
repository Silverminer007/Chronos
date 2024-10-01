package de.kjgstbarbara.service;

import de.kjgstbarbara.data.Date;
import de.kjgstbarbara.data.Group;
import de.kjgstbarbara.data.Person;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Stream;

public interface DateRepository extends JpaRepository<Date, Long> {
    List<Date> findByGroup(Group group);

    default Stream<Date> findByStartBetweenAndGroupMembersInAndGroupOrganisationMembersIn(LocalDateTime start, LocalDateTime end, Person... persons) {
        return findByStartBetweenAndGroupMembersInAndGroupOrganisationMembersIn(start, end, List.of(persons), List.of(persons)).stream();
    }

    List<Date> findByStartBetweenAndGroupMembersInAndGroupOrganisationMembersIn(LocalDateTime start, LocalDateTime end, List<Person> inGroup, List<Person> inOrg);

    default List<Date> calendarQuery(String searchTerm, int page, Person persons) {
        searchTerm = "%" + (searchTerm == null ? "" : searchTerm) + "%";
        LocalDateTime start = page < 0 ? LocalDateTime.now(ZoneOffset.UTC).minusYears(1000) : LocalDateTime.now(ZoneOffset.UTC);
        LocalDateTime end = page < 0 ? LocalDateTime.now(ZoneOffset.UTC) : LocalDateTime.now(ZoneOffset.UTC).plusYears(1000);
        return findByStartBetweenAndTitleLikeAndGroupMembersInAndGroupOrganisationMembersIn(
                start,
                end,
                searchTerm,
                List.of(persons),
                List.of(persons),
                PageRequest.of(page < 0 ? (page * -1) - 1 : page, 20, page >= 0
                        ? Sort.by("start").ascending()
                        : Sort.by("start").descending()
                )
        );
    }

    List<Date> findByStartBetweenAndTitleLikeAndGroupMembersInAndGroupOrganisationMembersIn(LocalDateTime start, LocalDateTime end, String searchTerm, List<Person> inGroup, List<Person> inOrg, PageRequest pageRequest);

    List<Date> findByLinkedTo(long linkedTo);

    List<Date> findByStartBetween(LocalDateTime after, LocalDateTime before);

    List<Date> findByPollScheduledFor(LocalDate localDate);
}
