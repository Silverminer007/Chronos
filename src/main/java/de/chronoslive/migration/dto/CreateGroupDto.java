package de.chronoslive.migration.dto;

import lombok.Data;

@Data
public class CreateGroupDto {
    private Long ownerId;
    private String groupName;
}
