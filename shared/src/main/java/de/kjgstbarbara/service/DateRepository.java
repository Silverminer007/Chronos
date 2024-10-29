package de.kjgstbarbara.service;

import de.kjgstbarbara.data.Date;
import de.kjgstbarbara.data.Group;
import de.kjgstbarbara.data.Organisation;
import de.kjgstbarbara.data.Person;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Stream;

public interface DateRepository extends JpaRepository<Date, Long> {
    List<Date> findByGroup(Group group);

    /*@Query("select d from Date d where (d.group.id in ?1 or d.group.organisation.id in ?2) and ?3 in d.group.members and d.start > ?4 and d.start < ?5")
    List<Date> findByGroupsOrganisationsIntervalAndPage(List<Long> groupId, List<Long> organisationId, long personId, LocalDateTime from, LocalDateTime to, Pageable pageable);

    //@Query("select d from Date d where d.group.organisation.id in ?1 and ?2 in d.group.members and d.start > ?3 and d.start < ?4")
    List<Date> findByOrganisationsIntervalAndPage(List<Long> organisationId, long personId, LocalDateTime from, LocalDateTime to, Pageable pageable);

    //@Query("select d from Date d where d.group.id in ?1 and ?2 in d.group.members and d.start > ?3 and d.start < ?4")
    List<Date> findByGroupsIntervalAndPage(List<Long> groupId, long personId, LocalDateTime from, LocalDateTime to, Pageable pageable);

    @Query("select d from Date d where ?1 in d.group.members and d.start > ?2 and d.start < ?3")
    List<Date> findByIntervalAndPage(long personId, LocalDateTime from, LocalDateTime to, Pageable pageable);
*/
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

    List<Date> findByStartBetweenAndGroupMembersIn(LocalDateTime after, LocalDateTime before, List<Person> inGroup);

    List<Date> findByGroupMembersIn(List<Person> inGroup);

    List<Date> findByPollScheduledFor(LocalDate localDate);

    long countByGroup(Group group);
    long countByGroupOrganisation(Organisation organisation);
}
