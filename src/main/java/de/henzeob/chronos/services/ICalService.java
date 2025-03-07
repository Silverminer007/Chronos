package de.henzeob.chronos.services;

import de.henzeob.chronos.entities.Date;
import de.henzeob.chronos.exceptions.InvalidDateException;
import org.springframework.stereotype.Service;

@Service
public class ICalService {
    public String parseICalDate(Date date) {
        return "";
    }

    public Date parseICalDate(String date) throws InvalidDateException {
        return null;
    }
}
