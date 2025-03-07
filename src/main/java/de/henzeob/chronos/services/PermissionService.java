package de.henzeob.chronos.services;

import de.henzeob.chronos.entities.Date;
import org.springframework.stereotype.Service;

@Service
public class PermissionService {
    public boolean canCancelDate(long dateId) {
        return true;
    }

    public boolean canRescheduleDate(long dateId) {
        return true;
    }

    public boolean canSendBroadcastInfo(long dateId) {
        return true;
    }

    public boolean canReadFeedback(long dateId) {
        return canReadDate(dateId);
    }

    public boolean canWriteFeedback(long dateId) {
        return true;
    }

    public boolean canReadDate(long dateId) {
        return true;
    }

    public boolean canWriteDate(long dateId) {
        return true;
    }

    public boolean canCreateDate(Date date) {
        return true;
    }

    public boolean canDeleteDate(long dateId) {
        return true;
    }

    public boolean canCreateComment(long dateId) {
        return canReadDate(dateId);
    }

    public boolean canReadComments(long dateId) {
        return canReadDate(dateId);
    }

    public boolean canReadHistoryStack(long dateId) {
        return canReadDate(dateId);
    }

    public boolean canStartDatePoll(long dateId) {
        return canReadDate(dateId);
    }

    public boolean canEndDatePoll(long dateId) {
        return canStartDatePoll(dateId);
    }

    public boolean canResetPoll(long dateId) {
        return canStartDatePoll(dateId);
    }

    public boolean canObserveDate(long dateId) {
        return canReadDate(dateId);
    }
}
