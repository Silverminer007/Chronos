package de.henzeob.chronos.services;

import de.henzeob.chronos.exceptions.ActionNotPermittedException;
import de.henzeob.chronos.exceptions.InvalidDateIdException;
import org.springframework.scheduling.annotation.Scheduled;

public class PollService {
    public void createPoll(long dateId) throws InvalidDateIdException, ActionNotPermittedException {
        // Check if date exists
        // Check permission
        // Read date
        // Update date
        // save date
        // Add entry to HistoryStack
        // Send Notification
    }

    public void endPoll(long dateId) throws InvalidDateIdException, ActionNotPermittedException {
        // Check if date exists
        // Check permission
        // Read date
        // Update date
        // save date
        // Add entry to HistoryStack
    }

    public void resetPoll() {
        // Check if date exists
        // Check permission
        // Read date
        // Update date
        // save date
        // Add entry to HistoryStack
        // Send Notification
    }

    @Scheduled(cron = "0 */15 * * * *")
    private void checkForOpenPollReminders() {}
}