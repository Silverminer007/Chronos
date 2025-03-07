package de.henzeob.chronos.services;

import org.springframework.scheduling.annotation.Scheduled;

public class ReminderService {
    @Scheduled(cron = "0 */15 * * * *")
    private void checkForOpenDateReminders() {

    }
}
