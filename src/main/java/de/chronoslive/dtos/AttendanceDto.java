package de.chronoslive.dtos;

public record AttendanceDto(Long id, Long user_id, Long date_id, String status, String lastChanged) {
}
