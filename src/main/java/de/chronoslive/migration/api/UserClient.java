package de.chronoslive.migration.api;

import de.chronoslive.migration.dto.CreateUserDto;
import de.chronoslive.migration.dto.UserDto;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.PostExchange;

public interface UserClient {
    @PostExchange("/api/v2/admin/user")
    UserDto createUser(@RequestBody CreateUserDto dto);
}
