package de.chronoslive.migration.api;

import de.chronoslive.migration.dto.FriendGroup;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.PostExchange;

public interface FriendshipClient {
    @PostExchange("/api/v2/admin/friendship/befriend")
    void befriend(@RequestBody FriendGroup friendGroup);
}
