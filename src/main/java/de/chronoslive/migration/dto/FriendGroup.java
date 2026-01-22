package de.chronoslive.migration.dto;

import lombok.Data;

import java.util.List;

@Data
public class FriendGroup {
    private List<Long> userIds;
}
