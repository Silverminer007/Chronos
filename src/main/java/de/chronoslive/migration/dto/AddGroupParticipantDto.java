package de.chronoslive.migration.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AddGroupParticipantDto {
    private Long group_id;
    private String user_role;
}
