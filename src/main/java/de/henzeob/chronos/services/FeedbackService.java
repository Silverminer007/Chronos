package de.henzeob.chronos.services;

import de.henzeob.chronos.entities.Feedback;
import de.henzeob.chronos.exceptions.ActionNotPermittedException;
import de.henzeob.chronos.exceptions.InvalidDateIdException;
import de.henzeob.chronos.exceptions.InvalidPersonIdException;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class FeedbackService {
    public Feedback getFeedbackOfPersonForDate(long dateId, long personId) throws InvalidDateIdException, InvalidPersonIdException, ActionNotPermittedException {
        // Check permission canReadFeedback
        // Read Feedback from database
        // return Feedback
        return null;
    }

    public Feedback getFeedbackOfPrincipalForDate(long dateId) throws InvalidDateIdException {
        // Principal can always read its own feedback, so no check required
        // Read feedback from database
        // return feedback
        return null;
    }

    public void submitFeedbackForDateOfPrincipal(long dateId, @NotNull Feedback feedback) throws ActionNotPermittedException, InvalidDateIdException {
        // Check Feedback for null
        // Check permission canWriteFeedback
        // Check if date exists
        // wrap feedback in Feedback Info
        // write FeedbackInfo to database
        // Add Info to HistoryStack
        // Send Notification
    }

    public List<Long> getAttendingPersonsForDate(long dateId) throws InvalidDateIdException, ActionNotPermittedException {
        // Check permission canReadFeedback
        // Read list of participants
        // Read feedback for each participant
        // filter list of participants by positive feedbacks
        // return list of participants ids
        return null;
    }

    public List<Long> getNotAttendingPersonsForDate(long dateId) throws InvalidDateIdException, ActionNotPermittedException {
        // Check permission canReadFeedback
        // Read list of participants
        // Read feedback for each participant
        // filter list of participants by negative feedbacks
        // return list of participants ids
        return null;
    }
}
