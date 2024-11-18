package de.kjgstbarbara.service;

import de.kjgstbarbara.data.Date;
import de.kjgstbarbara.data.PollReminder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PollReminderRepository extends JpaRepository<PollReminder, Long> {
    List<PollReminder> findByPollIntervalStartAfterAndPollIntervalEndBefore(LocalDateTime start, LocalDateTime end);
    Optional<PollReminder> findByDate(Date date);
}