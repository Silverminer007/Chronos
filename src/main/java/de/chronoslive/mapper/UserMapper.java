package de.chronoslive.mapper;

import de.chronoslive.dtos.UserDto;
import de.chronoslive.entitys.Person;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZoneId;

@Mapper(componentModel = "spring")
public interface UserMapper {
    Logger LOGGER = LoggerFactory.getLogger(UserMapper.class);

    @Mapping(source = "firstName", target = "first_name")
    @Mapping(source = "lastName", target = "last_name")
    @Mapping(source = "EMailAddress", target = "email")
    @Mapping(source = "phoneNumber", target = "phone_number")
    UserDto toDto(Person person);

    @Mapping(target = "firstName", source = "first_name")
    @Mapping(target = "lastName", source = "last_name")
    @Mapping(target = "EMailAddress", source = "email")
    @Mapping(target = "phoneNumber", source = "phone_number")
    Person toEntity(UserDto userDto);

    default String map(ZoneId zoneId) {
        return zoneId == null ? null : zoneId.getId();
    }

    default ZoneId mapZoneId(String zoneId) {
        return zoneId == null ? null : ZoneId.of(zoneId);
    }

    default String map(Person.PhoneNumber phoneNumber) {
        if (phoneNumber == null) return null;
        return phoneNumber.toString();
    }

    default Person.PhoneNumber mapPhoneNumber(String phoneNumber) {
        try {
            return phoneNumber == null ? null : Person.PhoneNumber.parse(phoneNumber);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Ignoring invalid phone number: {}", phoneNumber);
            return null;
        }
    }
}
