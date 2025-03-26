package de.chronoslive.repositorys;

import de.chronoslive.entitys.Date;
import de.chronoslive.entitys.PollReminder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PollReminderRepository extends JpaRepository<PollReminder, Long> {
    List<PollReminder> findByPollIntervalStartBeforeAndDateStartAfter(LocalDateTime start, LocalDateTime end);
    Optional<PollReminder> findByDate(Date date);
}