package de.chronoslive.migration.dto;

import lombok.Data;

@Data
public class CreateUserDto {
    private String firstName, lastName, email, oidcId;
}
