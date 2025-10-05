package de.chronoslive.mapper;

import de.chronoslive.dtos.AttendanceDto;
import de.chronoslive.entitys.Feedback;
import de.chronoslive.entitys.Person;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDateTime;

@Mapper(componentModel = "spring")
public interface AttendanceMapper {
    @Mapping(source = "person.id", target = "user_id")
    @Mapping(source = "timeStamp", target = "lastChanged")
    AttendanceDto toDto(Feedback feedback);

    @Mapping(source = "user_id", target = "person")
    @Mapping(source = "lastChanged", target = "timeStamp")
    Feedback toEntity(AttendanceDto dto);

    default LocalDateTime map(String isoDateTime) {
        return LocalDateTime.parse(isoDateTime);
    }

    default String map(LocalDateTime date) {
        return date.toString();
    }

    default Person map(long id) {
        return new Person(id);
    }

    default Feedback.Status mapStatus(String status) {
        return Feedback.Status.valueOf(status);
    }

    default String map(Feedback.Status status) {
        return status.toString();
    }
}