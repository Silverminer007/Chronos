package de.henzeob.chronos.services;

import de.henzeob.chronos.entities.Date;
import de.henzeob.chronos.exceptions.ActionNotPermittedException;
import de.henzeob.chronos.exceptions.InvalidDateException;
import de.henzeob.chronos.exceptions.InvalidDateIdException;
import org.springframework.stereotype.Service;

@Service
public class DateService {
    private final PermissionService permissionService;
    public DateService(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    public long createDate(Date date) throws InvalidDateException, ActionNotPermittedException {
        if(!this.permissionService.canCreateDate(date)) {
            throw new ActionNotPermittedException();
        }
        if(!this.validateDate(date)) {
            throw new InvalidDateException();
        }

        // Check permission canCreateDate
        // Validate date
        // write date to database
        // return date id provided by database
        // Add entry to HistoryStack
        return 0;
    }

    public Date readDate(String dateId) throws InvalidDateIdException, ActionNotPermittedException {
        // Check permission canReadDate
        // Read date from database
        // return date
        return null;
    }

    public Date updateDate(Date date) throws InvalidDateException, ActionNotPermittedException {
        // Check permission canWriteDate
        // Validate date
        // return updated date
        // Add entry to HistoryStack
        return date;
    }

    public void deleteDate(String dateId) throws InvalidDateIdException, ActionNotPermittedException {
        // Check permission canDeleteDate
        // Check if date exists
        // delete date
        // Add entry to HistoryStack
        // Send Notification
    }

    private boolean validateDate(Date date) {
        return true;
    }
}