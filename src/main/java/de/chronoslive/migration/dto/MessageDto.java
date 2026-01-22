package de.chronoslive.migration.dto;

import lombok.Data;

@Data
public class MessageDto {
    private Long id, sender_id, appointment_id;
    private String body, timestamp;
}
