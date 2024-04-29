package de.kjgstbarbara.service;

import de.kjgstbarbara.data.Reminder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReminderRepository extends JpaRepository<Reminder, Long> {
}