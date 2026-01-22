package de.chronoslive.migration.api;

import de.chronoslive.migration.dto.CreateGroupDto;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.PostExchange;

public interface GroupClient {

    @PostExchange("/api/v2/admin/groups/")
    Long createGroup(@RequestBody CreateGroupDto createGroupDto);

    @PostExchange("/api/v2/admin/groups/{groupId}/users/{userId}")
    void addGroupMember(@PathVariable Long groupId, @PathVariable Long userId);
}
