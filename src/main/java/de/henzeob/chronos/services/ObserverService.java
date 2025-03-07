package de.henzeob.chronos.services;

import de.henzeob.chronos.exceptions.ActionNotPermittedException;
import de.henzeob.chronos.exceptions.InvalidDateIdException;

public class ObserverService {
    public void observeDate(long dateId) throws InvalidDateIdException, ActionNotPermittedException {
        // Check if date exists
        // Check permission
        // save info to database
    }

    public void stopObservingDate(long dateId) throws InvalidDateIdException {
        // Check if date exists
        // Save info to database
    }

    public boolean doesObserveDate(long dateId) throws InvalidDateIdException {
        // Check if date exists
        // Read info from database
        return false;
    }
}