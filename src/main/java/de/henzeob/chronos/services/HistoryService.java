package de.henzeob.chronos.services;

import de.henzeob.chronos.entities.HistoryInfo;
import de.henzeob.chronos.exceptions.ActionNotPermittedException;
import de.henzeob.chronos.exceptions.InvalidDateIdException;

import java.util.List;

public class HistoryService {
    void pushHistoryStack(long dateId, String message) throws InvalidDateIdException {
        // Check if date exists
        // Populate HistoryInfo object with principalId, date-time, dateId und message
        // Save HistoryInfo to database
    }

    public void submitCommentForDate(long dateId, String message) throws InvalidDateIdException, ActionNotPermittedException {
        // Check permission canCreateComment
        // Send Message to pushHistoryStack
    }

    public List<HistoryInfo> getHistoryStack(long dateId) throws InvalidDateIdException {
        // Check for permission canReadHistoryStack
        // Check if date exists
        // Read all HistoryInfo from database with dateId
        // Sort HistoryInfo by dateTime
        // return History Infos
        return null;
    }
}
