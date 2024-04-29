package de.kjgstbarbara.service;

import de.kjgstbarbara.data.Person;
import de.kjgstbarbara.data.Reminder;
import lombok.Getter;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReminderService {
    private final ReminderRepository reminderRepository;

    public ReminderService(ReminderRepository reminderRepository) {
        this.reminderRepository = reminderRepository;
    }

    public List<Reminder> getReminders(Person person) {
        return reminderRepository.findAll().stream().filter(reminder -> reminder.getPerson() != null && reminder.getPerson().equals(person)).sorted().toList();
    }

    public void addReminder(Reminder reminder) {
        reminderRepository.save(reminder);
    }

    public void removeReminder(Reminder reminder) {
        reminderRepository.delete(reminder);
    }
}
