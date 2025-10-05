package de.chronoslive.dtos;

public record UserDto(Long id, String first_name, String last_name, String email, String timezone, String phone_number) {
}
