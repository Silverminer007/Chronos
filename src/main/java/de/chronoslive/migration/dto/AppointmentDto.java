package de.chronoslive.migration.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class AppointmentDto {

    // Pflichtfelder
    private Long id;

    private String name;

    private String description;

    private String start;

    private String end;

    private String venue;

    private Integer minimal_attendees;
}