package de.henzeob.chronos.services;

import de.henzeob.chronos.entities.Feedback;
import de.henzeob.chronos.entities.Notification;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class NotificationService {
    void sendNotification(Notification notification) {}
    void sendFeedbackUpdatedMessage(long dateId, @NotNull Feedback feedback) {}
    void sendDateCancelledMessage(long dateId) {}
    void sendDateRescheduledMessage(long dateId, LocalDateTime newStartDateTime, @Nullable String reason) {}
    void sendDateDeletedMessage(long dateId) {}
    void sendDatePollReminderMessage(long dateId, long personId) {}
    void sendDatePollCreatedMessage(long dateId) {}
    void sendDatePollResetMessage(long dateId) {}
    void sendDateReminderMessage(long dateId, long personId) {}
}