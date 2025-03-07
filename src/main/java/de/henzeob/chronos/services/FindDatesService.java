package de.henzeob.chronos.services;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class FindDatesService {
    public List<Long> findDates() {
        return null;
    }

    public List<Long> findDatesInFuture() {
        return null;
    }

    public List<Long> findDatesInPast() {
        return null;
    }

    public List<Long> findDatesInRange(LocalDateTime start, LocalDateTime end) {
        return null;
    }
}
