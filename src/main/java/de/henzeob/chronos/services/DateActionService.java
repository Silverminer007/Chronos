package de.henzeob.chronos.services;

import de.henzeob.chronos.exceptions.ActionNotPermittedException;
import de.henzeob.chronos.exceptions.InvalidDateIdException;
import jakarta.annotation.Nullable;

import java.time.LocalDateTime;

public class DateActionService {
    /**
     *
     * @param dateId
     * @return if the date was actually cancelled. Returns false if the date was already cancelled before
     * @throws ActionNotPermittedException
     * @throws InvalidDateIdException
     */
    public boolean cancelDate(long dateId, @Nullable String reason) throws ActionNotPermittedException, InvalidDateIdException {
        // Check permission
        // Read date from DateService
        // Create Updated version of date
        // Send Notification to participants
        // Save date to DateService
        // Add Entry to HistoryStack
        // Send Notification
        return false;
    }

    public void rescheduleDate(long dateId, LocalDateTime newStartDateTime, @Nullable String reason) throws ActionNotPermittedException, InvalidDateIdException {
        // Check permission
        // Read date from DateService
        // Create updated copy of date
        // Send Notifications to participants
        // Save updated version to DateService
        // Add entry to history stack
        // Send Notification
        // Possible reset of poll is handled by endpoints or UI
    }
}
