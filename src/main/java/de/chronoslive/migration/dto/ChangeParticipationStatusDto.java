package de.chronoslive.migration.dto;

import lombok.Data;

@Data
public class ChangeParticipationStatusDto {
    private Long userId, appointmentId;
    private String participationStatus;
}
